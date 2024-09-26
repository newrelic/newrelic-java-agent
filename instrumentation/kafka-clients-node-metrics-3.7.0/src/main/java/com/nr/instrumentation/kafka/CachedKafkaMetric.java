package com.nr.instrumentation.kafka;

public interface CachedKafkaMetric {
    boolean isValid();

    String displayName();

    void report(final FiniteMetricRecorder recorder);
}
