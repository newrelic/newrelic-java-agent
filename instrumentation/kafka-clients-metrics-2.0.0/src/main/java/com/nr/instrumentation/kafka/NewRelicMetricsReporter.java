/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class NewRelicMetricsReporter implements MetricsReporter {

    private static final boolean kafkaMetricsDebug = NewRelic.getAgent().getConfig().getValue("kafka.metrics.debug.enabled", false);

    private static final boolean metricsAsEvents = NewRelic.getAgent().getConfig().getValue("kafka.metrics.as_events.enabled", false);

    private static final long reportingIntervalInSeconds = NewRelic.getAgent().getConfig().getValue("kafka.metrics.interval", 30);

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, buildThreadFactory("NewRelicMetricsReporter-%d"));

    private final Map<String, KafkaMetric> metrics = new ConcurrentHashMap<>();

    @Override
    public void init(final List<KafkaMetric> initMetrics) {
        for (KafkaMetric kafkaMetric : initMetrics) {
            String metricGroupAndName = getMetricGroupAndName(kafkaMetric);
            if (kafkaMetricsDebug) {
                AgentBridge.getAgent().getLogger().log(Level.FINEST, "init(): {0} = {1}", metricGroupAndName, kafkaMetric.metricName());
            }
            metrics.put(metricGroupAndName, kafkaMetric);
        }

        final String metricPrefix = "MessageBroker/Kafka/Internal/";
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> eventData = new HashMap<>();
                    for (final Map.Entry<String, KafkaMetric> metric : metrics.entrySet()) {
                        Object metricValue = metric.getValue().metricValue();
                        if (metricValue instanceof Number) {
                            final float value = ((Number) metricValue).floatValue();
                            if (kafkaMetricsDebug) {
                                AgentBridge.getAgent().getLogger().log(Level.FINEST, "getMetric: {0} = {1}", metric.getKey(), value);
                            }
                            if (!Float.isNaN(value) && !Float.isInfinite(value)) {
                                if (metricsAsEvents) {
                                    eventData.put(metric.getKey().replace('/', '.'), value);
                                } else {
                                    NewRelic.recordMetric(metricPrefix + metric.getKey(), value);
                                }
                            }
                        }
                    }

                    if (metricsAsEvents) {
                        NewRelic.getAgent().getInsights().recordCustomEvent("KafkaMetrics", eventData);
                    }
                } catch (ConcurrentModificationException cme) {
                    // This is fixed in 1.0.3 and above but since this currently supports 0.11.0.0 we need to keep it here for now
                    // https://issues.apache.org/jira/browse/KAFKA-4950
                    // https://github.com/apache/kafka/pull/3907
                } catch (Exception e) {
                    AgentBridge.getAgent().getLogger().log(Level.FINE, e, "Unable to record kafka metrics");
                }
            }
        }, 0L, reportingIntervalInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void metricChange(final KafkaMetric metric) {
        String metricGroupAndName = getMetricGroupAndName(metric);
        if (kafkaMetricsDebug) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "metricChange(): {0} = {1}", metricGroupAndName, metric.metricName());
        }
        metrics.put(metricGroupAndName, metric);
    }

    @Override
    public void metricRemoval(final KafkaMetric metric) {
        String metricGroupAndName = getMetricGroupAndName(metric);
        if (kafkaMetricsDebug) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "metricRemoval(): {0} = {1}", metricGroupAndName, metric.metricName());
        }
        metrics.remove(metricGroupAndName);
    }

    private String getMetricGroupAndName(final KafkaMetric metric) {
        if (metric.metricName().tags().containsKey("topic")) {
            // Special case for handling topic names in metrics
            return metric.metricName().group() + "/" + metric.metricName().tags().get("topic") + "/" + metric.metricName().name();
        }
        return metric.metricName().group() + "/" + metric.metricName().name();
    }

    @Override
    public void close() {
        executor.shutdown();
        metrics.clear();
    }

    @Override
    public void configure(final Map<String, ?> configs) {
    }

    private static ThreadFactory buildThreadFactory(final String nameFormat) {
        // fail fast if the format is invalid
        String.format(nameFormat, 0);

        final ThreadFactory factory = Executors.defaultThreadFactory();
        final AtomicInteger count = new AtomicInteger();

        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                final Thread thread = factory.newThread(runnable);
                thread.setName(String.format(nameFormat, count.incrementAndGet()));
                thread.setDaemon(true);
                return thread;
            }
        };
    }

}
