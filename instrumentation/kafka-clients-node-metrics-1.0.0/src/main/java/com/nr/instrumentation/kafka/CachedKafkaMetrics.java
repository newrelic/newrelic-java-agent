package com.nr.instrumentation.kafka;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Measurable;

import java.util.regex.Pattern;

class CachedKafkaMetrics {

    static CachedKafkaMetric newCachedKafkaMetric(final KafkaMetric metric) {
        if ("app-info".equals(metric.metricName().group()) && "version".equals(metric.metricName().name())) {
            return new CachedKafkaVersion(metric);
        }

        Measurable measurable = null;
        try {
            measurable = metric.measurable();
        } catch (final IllegalStateException e) {
        }

        final boolean isCumulativeSumType = measurable != null &&
                CumulativeSumSupport.isCumulativeSumClass(measurable.getClass().getName());
        if (isCumulativeSumType) {
            return new CachedKafkaCounter(metric);
        }

        if (!(metric.metricValue() instanceof Number)) {
            return new InvalidCachedKafkaMetric(metric);
        }

        return new CachedKafkaSummary(metric);
    }

    private static class CachedKafkaVersion implements CachedKafkaMetric {
        private final KafkaMetric metric;
        private final String newRelicMetricName;

        public CachedKafkaVersion(final KafkaMetric metric) {
            this.metric = metric;
            this.newRelicMetricName = MetricNameUtil.METRIC_PREFIX + "app-info/version/" + metric.metricValue();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public String displayName() {
            return "app-info/version/" + metric.metricValue();
        }

        @Override
        public void report(final FiniteMetricRecorder recorder) {
            recorder.recordMetric(newRelicMetricName, 1.0f);
        }
    }

    private static class InvalidCachedKafkaMetric implements CachedKafkaMetric {
        private final KafkaMetric metric;

        public InvalidCachedKafkaMetric(final KafkaMetric metric) {
            this.metric = metric;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public String displayName() {
            return MetricNameUtil.buildDisplayName(metric);
        }

        @Override
        public void report(FiniteMetricRecorder recorder) {
            // no-op
        }
    }

    private static class CachedKafkaSummary implements CachedKafkaMetric {
        private final KafkaMetric metric;
        private final String newRelicMetricName;

        public CachedKafkaSummary(final KafkaMetric metric) {
            this.metric = metric;
            this.newRelicMetricName = MetricNameUtil.buildMetricName(metric);
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public String displayName() {
            return MetricNameUtil.buildDisplayName(metric);
        }

        @Override
        public void report(final FiniteMetricRecorder recorder) {
            recorder.recordMetric(newRelicMetricName, ((Number) metric.metricValue()).floatValue());
        }
    }

    private static class CachedKafkaCounter implements CachedKafkaMetric {
        private final KafkaMetric metric;
        private static final Pattern totalPattern = Pattern.compile("-total$");

        private final String counterMetricName;
        private final String totalMetricName;

        private int previous = -1;

        public CachedKafkaCounter(final KafkaMetric metric) {
            this.metric = metric;

            totalMetricName = MetricNameUtil.buildMetricName(metric);

            String metricName = metric.metricName().name();
            String counterName = totalPattern.matcher(metricName).replaceAll("-counter");
            if (counterName.equals(metricName)) {
                counterName = metricName + "-counter";
            }
            counterMetricName = MetricNameUtil.buildMetricName(metric, counterName);
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public String displayName() {
            return MetricNameUtil.buildDisplayName(metric);
        }

        @Override
        public void report(final FiniteMetricRecorder recorder) {
            final Number value = ((Number) metric.metricValue());
            if (!recorder.tryRecordMetric(totalMetricName, value.floatValue())) {
                // we can't trust the last observed value, so reset
                previous = -1;
                return;
            }

            final int intValue = value.intValue();
            if (previous == -1L) {
                previous = intValue;
                return;
            }

            final int delta = intValue - previous;
            previous = intValue;

            recorder.incrementCounter(counterMetricName, delta);
        }
    }

    private CachedKafkaMetrics() {
        // prevents instantiation
    }
}
