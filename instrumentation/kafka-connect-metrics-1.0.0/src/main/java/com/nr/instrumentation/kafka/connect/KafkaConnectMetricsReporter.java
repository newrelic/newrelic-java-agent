/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka.connect;

import com.newrelic.api.agent.NewRelic;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.connect.runtime.ConnectMetrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class KafkaConnectMetricsReporter implements org.apache.kafka.common.metrics.MetricsReporter {

    private static final String KAFKA_CONNECT_METRICS_EVENT_NAME = "KafkaConnectMetrics";

    private static final String METRIC_PREFIX = "Kafka/Connect/";

    private static final boolean KAFKA_METRICS_DEBUG = NewRelic.getAgent().getConfig().getValue("kafka.metrics.debug.enabled", false);

    private static final boolean METRICS_AS_EVENTS = NewRelic.getAgent().getConfig().getValue("kafka.metrics.as_events.enabled", false);

    private static final long REPORTING_INTERVAL_IN_SECONDS = NewRelic.getAgent().getConfig().getValue("kafka.metrics.interval", 30);

    private static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(1, buildThreadFactory());

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    // key is the metric/event name
    private final Map<String, KafkaMetric> metrics = new ConcurrentHashMap<>();

    private ScheduledFuture<?> scheduledFuture;

    // the metrics are shared between Workers. So only one reporter needs to be attached.
    public static void initialize(ConnectMetrics.MetricGroup metricGroup) {
        if (metricGroup != null && metricGroup.metrics() != null &&
                !INITIALIZED.getAndSet(true)) {
            metricGroup.metrics().addReporter(new KafkaConnectMetricsReporter());
        }
    }

    @Override
    public void init(final List<KafkaMetric> initMetrics) {
        for (KafkaMetric kafkaMetric : initMetrics) {
            String metricName = getMetricName(kafkaMetric);
            if (KAFKA_METRICS_DEBUG) {
                NewRelic.getAgent().getLogger().log(Level.FINEST,
                        "init(): {0} = {1}", metricName, kafkaMetric.metricName());
            }
            metrics.put(metricName, kafkaMetric);
        }
        scheduledFuture = EXECUTOR.scheduleAtFixedRate(
                this::harvest, 0L, REPORTING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }

    // default visibility for testing
    void harvest() {
        try {
            Map<String, Object> eventData = METRICS_AS_EVENTS ? new HashMap<>() : Collections.emptyMap();
            for (final Map.Entry<String, KafkaMetric> metric : metrics.entrySet()) {
                Object metricValue = metric.getValue().metricValue();
                if (metricValue instanceof Number) {
                    final float value = ((Number) metricValue).floatValue();
                    if (KAFKA_METRICS_DEBUG) {
                        NewRelic.getAgent().getLogger().log(Level.FINEST, "getMetric: {0} = {1}", metric.getKey(), value);
                    }
                    if (!Float.isNaN(value) && !Float.isInfinite(value)) {
                        if (METRICS_AS_EVENTS) {
                            eventData.put(metric.getKey(), value);
                        } else {
                            NewRelic.recordMetric(metric.getKey(), value);
                        }
                    }
                }
            }

            if (METRICS_AS_EVENTS && !eventData.isEmpty()) {
                NewRelic.getAgent().getInsights().recordCustomEvent(KAFKA_CONNECT_METRICS_EVENT_NAME, eventData);
            }
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINE, e, "Unable to record kafka metrics");
        }
    }

    @Override
    public void metricChange(final KafkaMetric metric) {
        String metricGroupAndName = getMetricName(metric);
        if (KAFKA_METRICS_DEBUG) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "metricChange(): {0} = {1}", metricGroupAndName, metric.metricName());
        }
        metrics.put(metricGroupAndName, metric);
    }

    @Override
    public void metricRemoval(final KafkaMetric metric) {
        String metricGroupAndName = getMetricName(metric);
        if (KAFKA_METRICS_DEBUG) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "metricRemoval(): {0} = {1}", metricGroupAndName, metric.metricName());
        }
        metrics.remove(metricGroupAndName);
    }

    /**
     * The metric/event name will be something in the format:
     * <pre>
     * {metricGroup}[[/{connector}]-{task}]/{metricName}
     * Where
     * [] = optional
     * {} = interpolated value.
     * </pre>
     */
    private String getMetricName(final KafkaMetric metric) {
        // events have different separator
        char separator = METRICS_AS_EVENTS ? '.' : '/';

        MetricName metricName = metric.metricName();
        Map<String, String> tags = metricName.tags();

        StringBuilder sb = new StringBuilder();
        // metrics have a prefix
        if (!METRICS_AS_EVENTS) {
            sb.append(METRIC_PREFIX);
        }

        sb.append(metricName.group());

        if (tags.containsKey("connector")) {
            sb.append(separator).append(tags.get("connector"));
            if (tags.containsKey("task")) {
                sb.append('-').append(tags.get("task"));
            }
        }

        sb.append(separator).append(metricName.name());
        return sb.toString();
    }

    @Override
    public void close() {
        scheduledFuture.cancel(false);
        INITIALIZED.set(false);
        metrics.clear();
    }

    @Override
    public void configure(final Map<String, ?> configs) {
    }

    private static ThreadFactory buildThreadFactory() {
        final AtomicInteger count = new AtomicInteger();

        return runnable -> {
            String name = String.format("NrKafkaConnectMetricsReporter-%d", count.incrementAndGet());
            final Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

}
