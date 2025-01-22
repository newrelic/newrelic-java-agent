/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Measurable;

import java.util.regex.Pattern;

class CachedKafkaMetrics {

    static CachedKafkaMetric newCachedKafkaMetric(final KafkaMetric metric) {
        if ("app-info".equals(metric.metricName().group()) && "version".equals(metric.metricName().name())) {
            return new CachedKafkaVersion(metric);
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

    private CachedKafkaMetrics() {
        // prevents instantiation
    }
}
