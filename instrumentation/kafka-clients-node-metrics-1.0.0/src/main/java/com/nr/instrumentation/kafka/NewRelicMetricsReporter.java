package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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
    private final NodeTopicRegistry nodeTopicRegistry;

    public NewRelicMetricsReporter(ClientType clientType, Collection<Node> nodes) {
        nodeTopicRegistry = new NodeTopicRegistry(clientType, nodes);
    }

    @Override
    public void init(List<KafkaMetric> metrics) {
        NewRelic.getAgent().getLogger().log(Level.INFO,
                "newrelic-kafka-clients-enhancements: initializing. This version of Kafka does not support cumulative sum.");

        for (final KafkaMetric metric : metrics) {
            registerMetric(metric);
        }

        future = SCHEDULER.scheduleAtFixedRate(this::report, 0, REPORTING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void metricChange(KafkaMetric metric) {
        registerMetric(metric);
    }

    @Override
    public void metricRemoval(KafkaMetric metric) {
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
        metrics.clear();
        nodeTopicRegistry.close();
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

        String topic = metric.metricName().tags().get("topic");
        if (TOPIC_METRICS_DISABLED && topic != null) {
            debugLog("newrelic-kafka-clients-enhancements: skipping topic metric registration: {0}",
                    MetricNameUtil.buildDisplayName(metric));
            return;
        }
        if (nodeTopicRegistry.register(topic)) {
            debugLog("newrelic-kafka-clients-enhancements: register node topic metric for topic: {0}", topic);
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

        nodeTopicRegistry.report(recorder);
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
