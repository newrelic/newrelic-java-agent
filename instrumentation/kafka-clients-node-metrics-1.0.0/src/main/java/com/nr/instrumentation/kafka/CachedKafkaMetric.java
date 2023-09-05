package com.nr.instrumentation.kafka;

interface CachedKafkaMetric {
    boolean isValid();

    String displayName();

    void report(final FiniteMetricRecorder recorder);
}
