package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;

public class NewRelicMetricsReporter implements MetricsReporter {

    private static final boolean METRICS_DEBUG = NewRelic.getAgent().getConfig().getValue("kafka.metrics.debug.enabled", false);
    private static final boolean NODE_METRICS_DISABLED = NewRelic.getAgent().getConfig().getValue("kafka.metrics.node.metrics.disabled", false);
    private static final boolean TOPIC_METRICS_DISABLED = NewRelic.getAgent().getConfig().getValue("kafka.metrics.topic.metrics.disabled", false);
    private static final long REPORTING_INTERVAL_IN_SECONDS = NewRelic.getAgent().getConfig().getValue("kafka.metrics.interval", 30);
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(ThreadFactories.build("NewRelicMetricsReporter-Kafka"));
    private ScheduledFuture<?> future;

    private final ConcurrentHashMap<MetricName, CachedKafkaMetric> metrics = new ConcurrentHashMap<>();
    private final FiniteMetricRecorder recorder = new FiniteMetricRecorder();

    @Override
    public void init(List<KafkaMetric> metrics) {
        NewRelic.getAgent().getLogger().log(Level.INFO,
                "newrelic-kafka-clients-enhancements: initializing with SUPPORTS_CUMULATIVE_SUM={0}",
                CumulativeSumSupport.isCumulativeSumSupported());

        for (final KafkaMetric metric : metrics) {
            registerMetric(metric);
        }

        future = SCHEDULER.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                report();
            }
        }, 0, REPORTING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void metricChange(KafkaMetric metric) {
        registerMetric(metric);
    }

    @Override
    public void metricRemoval(KafkaMetric metric) {
        metrics.remove(metric.metricName());

        final CachedKafkaMetric cachedMetric = metrics.remove(metric.metricName());
        if (cachedMetric != null) {
            debugLog("newrelic-kafka-clients-enhancements: deregister metric: {0}", cachedMetric.displayName());
        }
    }

    @Override
    public void close() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    @Override
    public void configure(Map<String, ?> configs) {

    }

    private void registerMetric(final KafkaMetric metric) {
        if (NODE_METRICS_DISABLED && metric.metricName().tags().get("node-id") != null) {
            debugLog("newrelic-kafka-clients-enhancements: skipping node metric registration: {0}",
                    MetricNameUtil.buildDisplayName(metric));
            return;
        }

        if (TOPIC_METRICS_DISABLED && metric.metricName().tags().get("topic") != null) {
            debugLog("newrelic-kafka-clients-enhancements: skipping topic metric registration: {0}",
                    MetricNameUtil.buildDisplayName(metric));
            return;
        }

        final CachedKafkaMetric cachedMetric = CachedKafkaMetrics.newCachedKafkaMetric(metric);
        if (cachedMetric.isValid()) {
            debugLog("newrelic-kafka-clients-enhancements: register metric: {0}", cachedMetric.displayName());

            this.metrics.put(metric.metricName(), cachedMetric);
        } else {
            debugLog("newrelic-kafka-clients-enhancements: skipping metric registration: {0}", cachedMetric.displayName());
        }
    }

    private void report() {
        debugLog("newrelic-kafka-clients-enhancements: reporting Kafka metrics");

        for (final CachedKafkaMetric metric : metrics.values()) {
            metric.report(recorder);
        }
    }

    private void debugLog(String message) {
        if (METRICS_DEBUG) {
            NewRelic.getAgent().getLogger().log(Level.INFO, message);
        }
    }

    private void debugLog(String message, Object value) {
        if (METRICS_DEBUG) {
            NewRelic.getAgent().getLogger().log(Level.INFO, message, value);
        }
    }
}
