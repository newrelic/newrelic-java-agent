package com.nr.instrumentation.kafka.streams;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import org.apache.kafka.common.metrics.KafkaMetric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static com.nr.instrumentation.kafka.streams.MetricsConstants.KAFKA_METRICS_DEBUG;
import static com.nr.instrumentation.kafka.streams.MetricsConstants.METRICS_AS_EVENTS;
import static com.nr.instrumentation.kafka.streams.MetricsConstants.METRICS_EVENT_TYPE;
import static com.nr.instrumentation.kafka.streams.MetricsConstants.METRIC_PREFIX;
import static com.nr.instrumentation.kafka.streams.MetricsConstants.REPORTING_INTERVAL_IN_SECONDS;

public class MetricsScheduler {
    private static ScheduledThreadPoolExecutor executor = null;
    private static final Set<NewRelicMetricsReporter> metricReporters = new HashSet<>();
    private static final Object lock = new Object();

    private MetricsScheduler() {}

    public static void addMetricsReporter(NewRelicMetricsReporter metricsReporter) {
        synchronized (lock) {
            metricReporters.add(metricsReporter);
            if (executor != null) {
                return;
            }
            executor = createScheduledExecutor();
            executor.scheduleAtFixedRate(new MetricsSendRunnable(), 0L, REPORTING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
        }
    }

    public static void removeMetricsReporter(NewRelicMetricsReporter metricsReporter) {
        synchronized (lock) {
            metricReporters.remove(metricsReporter);
            if (metricReporters.isEmpty()) {
                executor.shutdown();
                executor = null;
            }
        }
    }

    private static ScheduledThreadPoolExecutor createScheduledExecutor() {
        return new ScheduledThreadPoolExecutor(1, buildThreadFactory("NewRelicMetricsReporter-KafkaStreams-%d"));
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

    private static class MetricsSendRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (lock) {
                for (NewRelicMetricsReporter nrMetricsReporter: metricReporters) {
                    sendMetrics(nrMetricsReporter);
                }
            }
        }
        private void sendMetrics(NewRelicMetricsReporter nrMetricsReporter) {
            try {
                Map<String, Object> eventData = new HashMap<>();
                for (final Map.Entry<String, KafkaMetric> metric : nrMetricsReporter.getMetrics().entrySet()) {
                    Object metricValue = metric.getValue().metricValue();
                    if (metricValue instanceof Double) {
                        final float value = ((Double) metricValue).floatValue();
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
                if (METRICS_AS_EVENTS) {
                    NewRelic.getAgent().getInsights().recordCustomEvent(METRICS_EVENT_TYPE, eventData);
                }
            } catch (Exception e) {
                AgentBridge.getAgent().getLogger().log(Level.FINE, e, "Unable to record kafka metrics");
            }
        }
    }
}

