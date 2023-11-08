package com.newrelic.agent.stats.dimensional;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class MetricAggregates implements Aggregator {
    private static final Charset CHARSET = Charsets.UTF_8;
    private final String metricName;
    private final MetricType metricType;
    private final Map<Long, AttributeMeasure> measuresByHash = new ConcurrentHashMap<>();

    MetricAggregates(String metricName, MetricType metricType) {
        this.metricName = metricName;
        this.metricType = metricType;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    @Override
    public Measure getMeasure(Map<String, Object> attributes) {
        return measuresByHash.computeIfAbsent(getHash(attributes), key -> createMeasure(attributes));
    }

    private AttributeMeasure createMeasure(final Map<String, Object> attributes) {
        switch (metricType) {
            case count:
                return new CountMeasure(attributes);
            default:
                return new SummaryMeasure(attributes);
        }
    }

    static long getHash(Map<String, Object> attributes) {
        if (attributes.isEmpty()) {
            return Long.MAX_VALUE;
        }
        final Hasher hasher = Hashing.murmur3_128().newHasher();
        if (attributes.size() == 1) {
            attributes.forEach((key, value) -> {
                hasher.putString(key, CHARSET);
                putValue(hasher, value);
            });
            return hasher.hash().asLong();
        }
        final List<String> keys = new ArrayList<>(attributes.keySet());
        // deterministically order keys
        keys.sort(Comparator.comparingInt(String::hashCode));
        keys.forEach(key -> {
            hasher.putString(key, CHARSET);
            putValue(hasher, attributes.get(key));
        });
        return hasher.hash().asLong();
    }

    static void putValue(Hasher hasher, Object value) {
        if (value instanceof Boolean) {
            hasher.putBoolean(((Boolean) value).booleanValue());
        } else if (value instanceof Number) {
            if (value instanceof Integer) {
                hasher.putInt(((Integer)value).intValue());
            } else if (value instanceof Long) {
                hasher.putLong(((Long)value).longValue());
            } else if (value instanceof Float) {
                hasher.putFloat(((Float)value).floatValue());
            } else if (value instanceof Double) {
                hasher.putDouble(((Double) value).doubleValue());
            } else {
                hasher.putString(value.toString(), CHARSET);
            }
        } else {
            hasher.putString(value.toString(), CHARSET);
        }
    }

    public void harvest(Collection<CustomInsightsEvent> events) {
        measuresByHash.values().forEach(measure -> events.add(measure.createEvent(metricType, metricName)));
    }

    MetricAggregates merge(MetricAggregates other) {
        if (other.metricType.equals(this.metricType)) {
            other.measuresByHash.forEach((id, measure) -> this.measuresByHash.compute(id,
                    (key, existing) -> existing == null ? measure : existing.merge(measure)));
        }
        return this;
    }

    abstract static class AttributeMeasure implements Measure {
        final Map<String, Object> attributes;
        final long timestamp = System.currentTimeMillis();

        public AttributeMeasure(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        CustomInsightsEvent createEvent(MetricType type, String metricName) {
            final long now = System.currentTimeMillis();
            final long durationMillis = now - timestamp;
            final Map<String, Object> intrinsics = ImmutableMap.of("metric.name", metricName,
                    type.attributeName(), getValue(), "duration.ms", durationMillis);
            return new CustomInsightsEvent("Metric", timestamp, this.attributes, intrinsics, DistributedTraceServiceImpl.nextTruncatedFloat());
        }

        abstract Object getValue();

        abstract AttributeMeasure merge(AttributeMeasure other);
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

    static class SummaryMeasure extends AttributeMeasure {

        final AtomicReference<Summary> summary = new AtomicReference<>();

        public SummaryMeasure(Map<String, Object> attributes) {
            super(attributes);
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
        AttributeMeasure merge(AttributeMeasure other) {
            if (other instanceof SummaryMeasure) {
                addToSummary(((SummaryMeasure) other).summary.get());
            }
            return this;
        }

        @Override
        public void addToSummary(double value) {
            addToSummary(new Summary(1, value, value, value));
        }

        private void addToSummary(Summary summary) {
            this.summary.accumulateAndGet(summary,
                    (existing, update) -> update.merge(existing));
        }

        @Override
        public void incrementCount(int count) {
            // error?
        }
    }

    static class CountMeasure extends AttributeMeasure {
        final AtomicInteger count = new AtomicInteger();
        public CountMeasure(Map<String, Object> attributes) {
            super(attributes);
        }

        @Override
        Object getValue() {
            return count.get();
        }

        @Override
        AttributeMeasure merge(AttributeMeasure other) {
            if (other instanceof CountMeasure) {
                this.count.addAndGet(((CountMeasure) other).count.get());
            }
            return this;
        }

        @Override
        public void incrementCount(int count) {
            this.count.addAndGet(count);
        }
    }
}
