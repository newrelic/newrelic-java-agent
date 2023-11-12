package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class AttributeBucket {
    private final Map<MeasureKey, BaseMeasure> keyToMeasures =
            new ConcurrentHashMap<>();
    private final Map<String, Object> attributes;

    public AttributeBucket(Map<String, ?> attributes) {
        this.attributes = ImmutableMap.copyOf(attributes);
    }

    public Measure getMeasure(String metricName, MetricType type) {
        return keyToMeasures.computeIfAbsent(new MeasureKey(metricName, type), key -> createMeasure(metricName, type));
    }

    BaseMeasure createMeasure(String metricName, MetricType type) {
        switch (type) {
            case summary:
                return new SummaryMeasure(metricName);
            default:
                return new CountMeasure(metricName);
        }
    }

    public AttributeBucket merge(AttributeBucket other) {
        other.keyToMeasures.forEach((key, measure) -> this.keyToMeasures.merge(key, measure, BaseMeasure::merge));
        return this;
    }

    public void harvest(Collection<CustomInsightsEvent> events) {
        keyToMeasures.values().forEach(measure -> events.add(measure.toEvent()));
    }

    abstract class BaseMeasure implements Measure {
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

        abstract BaseMeasure merge(BaseMeasure measure);
    }

    class SummaryMeasure extends BaseMeasure {
        private final AtomicReference<Summary> summary = new AtomicReference<>();

        public SummaryMeasure(String metricName) {
            super(metricName, MetricType.summary);
        }

        @Override
        public void addToSummary(double value) {
            addToSummary(new Summary(1, value, value, value));
        }

        private void addToSummary(Summary summary) {
            if (summary != null) {
                this.summary.accumulateAndGet(summary,
                        (existing, update) -> update.merge(existing));
            }
        }

        @Override
        Object getValue() {
            final Summary summary = this.summary.get();
            if (summary == null) {
                return ImmutableList.of(0, 0d, 0d, 0d);
            }
            return ImmutableList.of(summary.count, summary.total, summary.min, summary.max);
        }

        @Override
        BaseMeasure merge(BaseMeasure measure) {
            if (measure.type == MetricType.summary) {
                addToSummary(((SummaryMeasure)measure).summary.get());
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

        @Override
        public void incrementCount(long count) {
            this.count.addAndGet(count);
        }
    }

    static class Summary {
        final long count;
        final double total;
        final double min;
        final double max;

        public Summary(long count, double total, double min, double max) {
            this.count = count;
            this.total = total;
            this.min = min;
            this.max = max;
        }

        public Summary merge(Summary existing) {
            if (existing == null) {
                return this;
            }
            return new Summary(count + existing.count, total + existing.total,
                    Math.min(min, existing.min), Math.max(max, existing.max));
        }
    }

    static class MeasureKey {
        private final String metricName;
        private final MetricType type;

        public MeasureKey(String metricName, MetricType type) {
            this.metricName = metricName;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MeasureKey that = (MeasureKey) o;
            return Objects.equals(metricName, that.metricName) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(metricName, type);
        }
    }
}
