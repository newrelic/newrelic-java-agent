/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.kafka;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import org.apache.kafka.common.metrics.KafkaMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.nr.instrumentation.kafka.MetricsConstants.KAFKA_METRICS_DEBUG;
import static com.nr.instrumentation.kafka.MetricsConstants.METRICS_AS_EVENTS;
import static com.nr.instrumentation.kafka.MetricsConstants.METRICS_EVENT_TYPE;
import static com.nr.instrumentation.kafka.MetricsConstants.METRIC_PREFIX;
import static com.nr.instrumentation.kafka.MetricsConstants.REPORTING_INTERVAL_IN_SECONDS;

public class MetricsScheduler {
    private static final ScheduledExecutorService executor = createScheduledExecutor();
    private static final Map<NewRelicMetricsReporter, ScheduledFuture<?>> metricReporterTasks = new ConcurrentHashMap<>();

    private MetricsScheduler() {}

    public static void addMetricsReporter(NewRelicMetricsReporter metricsReporter) {
        ScheduledFuture<?> task = executor.scheduleAtFixedRate(new MetricsSendRunnable(metricsReporter),
                0L, REPORTING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
        metricReporterTasks.put(metricsReporter, task);
    }

    public static void removeMetricsReporter(NewRelicMetricsReporter metricsReporter) {
        ScheduledFuture<?> task = metricReporterTasks.remove(metricsReporter);
        task.cancel(false);
    }

    private static ScheduledExecutorService createScheduledExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("NewRelicMetricsReporter-Kafka");
            return thread;
        });
    }

    private static class MetricsSendRunnable implements Runnable {
        private final NewRelicMetricsReporter nrMetricsReporter;

        private MetricsSendRunnable(NewRelicMetricsReporter nrMetricsReporter) {
            this.nrMetricsReporter = nrMetricsReporter;
        }
        @Override
        public void run() {
            try {
                Map<String, Object> eventData = new HashMap<>();
                for (final Map.Entry<String, KafkaMetric> metric : nrMetricsReporter.getMetrics().entrySet()) {
                    Object metricValue = metric.getValue().metricValue();
                    if (metricValue instanceof Number) {
                        final float value = ((Number) metricValue).floatValue();
                        if (KAFKA_METRICS_DEBUG) {
                            AgentBridge.getAgent().getLogger().log(Level.FINEST, "getMetric: {0} = {1}", metric.getKey(), value);
                        }
                        if (!Float.isNaN(value) && !Float.isInfinite(value)) {
                            if (METRICS_AS_EVENTS) {
                                eventData.put(metric.getKey().replace('/', '.'), value);
                            } else {
                                NewRelic.recordMetric(METRIC_PREFIX + metric.getKey(), value);
                            }
                        }
                    }
                }

                for (NewRelicMetricsReporter.NodeMetricNames consumerNodeMetricNames : nrMetricsReporter.getNodes().values()) {
                    if (METRICS_AS_EVENTS) {
                        for (String eventName : consumerNodeMetricNames.getEventNames()) {
                            eventData.put(eventName, 1f);
                        }
                    } else {
                        for (String metricName : consumerNodeMetricNames.getMetricNames()) {
                            NewRelic.recordMetric(metricName, 1f);
                        }
                    }
                }

                if (METRICS_AS_EVENTS) {
                    NewRelic.getAgent().getInsights().recordCustomEvent(METRICS_EVENT_TYPE, eventData);
                }
            } catch (Exception e) {
                AgentBridge.getAgent().getLogger().log(Level.FINE, e, "Unable to record kafka metrics");
            }
        }
    }
}
