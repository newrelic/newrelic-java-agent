package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class AttributeBucket {
    private final Map<String, CountMeasure> nameToCounters =
            new ConcurrentHashMap<>();
    private final Map<String, SummaryMeasure> nameToSummaries =
            new ConcurrentHashMap<>();
    private final Map<String, Object> attributes;

    public AttributeBucket(Map<String, ?> attributes) {
        this.attributes = ImmutableMap.copyOf(attributes);
    }

    public CountMeasure getCounter(String metricName) {
        return nameToCounters.computeIfAbsent(metricName, key -> new CountMeasure(metricName));
    }

    public SummaryMeasure getSummary(String metricName) {
        return nameToSummaries.computeIfAbsent(metricName, key -> new SummaryMeasure(metricName));
    }

    public AttributeBucket merge(AttributeBucket other) {
        other.nameToSummaries.forEach((key, measure) -> this.nameToSummaries.merge(key, measure, BaseMeasure::merge));
        other.nameToCounters.forEach((key, measure) -> this.nameToCounters.merge(key, measure, BaseMeasure::merge));
        return this;
    }

    public void harvest(Collection<CustomInsightsEvent> events) {
        nameToSummaries.values().forEach(measure -> events.add(measure.toEvent()));
        nameToCounters.values().forEach(measure -> events.add(measure.toEvent()));
    }

    abstract class BaseMeasure {
        private final long beginTimestamp = System.currentTimeMillis();
        private final String metricName;
        private final MetricType type;

        public BaseMeasure(String metricName, MetricType type) {
            this.metricName = metricName;
            this.type = type;
        }

        public final CustomInsightsEvent toEvent() {
            final long now = System.currentTimeMillis();
            final long durationMillis = now - beginTimestamp;
            final Map<String, Object> intrinsics = ImmutableMap.of("metric.name", metricName,
                    type.attributeName(), getValue(), "duration.ms", durationMillis);
            return new CustomInsightsEvent("Metric", beginTimestamp, attributes, intrinsics, DistributedTraceServiceImpl.nextTruncatedFloat());
        }

        abstract Object getValue();

        abstract <T extends BaseMeasure> T merge(T measure);
    }

    class SummaryMeasure extends BaseMeasure {
        private final AtomicReference<SummaryValue> summary = new AtomicReference<>();

        public SummaryMeasure(String metricName) {
            super(metricName, MetricType.summary);
        }

        public void add(double value) {
            add(new SummaryValue(1, value, value, value));
        }

        private void add(SummaryValue summaryValue) {
            if (summaryValue != null) {
                this.summary.accumulateAndGet(summaryValue,
                        (existing, update) -> update.merge(existing));
            }
        }

        @Override
        Object getValue() {
            final SummaryValue summaryValue = this.summary.get();
            if (summaryValue == null) {
                return ImmutableList.of(0, 0d, 0d, 0d);
            }
            return ImmutableList.of(summaryValue.count, summaryValue.total, summaryValue.min, summaryValue.max);
        }

        @Override
        SummaryMeasure merge(BaseMeasure measure) {
            if (measure.type == MetricType.summary) {
                add(((SummaryMeasure)measure).summary.get());
            }
            return this;
        }
    }

    class CountMeasure extends BaseMeasure {
        private final AtomicLong count = new AtomicLong();

        public CountMeasure(String metricName) {
            super(metricName, MetricType.count);
        }

        @Override
        Object getValue() {
            return count.get();
        }

        @Override
        BaseMeasure merge(BaseMeasure measure) {
            if (measure.type == MetricType.count) {
                this.count.addAndGet(((CountMeasure)measure).count.get());
            }
            return this;
        }

        public void add(long count) {
            this.count.addAndGet(count);
        }
    }

    static class SummaryValue {
        final long count;
        final double total;
        final double min;
        final double max;

        public SummaryValue(long count, double total, double min, double max) {
            this.count = count;
            this.total = total;
            this.min = min;
            this.max = max;
        }

        public SummaryValue merge(SummaryValue existing) {
            if (existing == null) {
                return this;
            }
            return new SummaryValue(count + existing.count, total + existing.total,
                    Math.min(min, existing.min), Math.max(max, existing.max));
        }
    }
}
