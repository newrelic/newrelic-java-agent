/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.opentelemetry;

import com.newrelic.api.agent.MetricAggregator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.newrelic.opentelemetry.OpenTelemetryNewRelic.SCOPE_NAME;

final class OpenTelemetryMetricsAggregator implements MetricAggregator {

    private static final double NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    // Rename because customers expect to query newrelic.timeslice.value. Would be ideal to use for counter as well, but can't have different types.
    private static final String NEWRELIC_TIMESLICE_HISTOGRAM_VALUE_METRIC = "newrelic.timeslice.value";
    private static final String NEWRELIC_TIMESLICE_COUNTER_VALUE_METRIC = "newrelic.timeslice.counter.value";
    private static final AttributeKey<String> NEWRELIC_TIMESLICE_ATTRIBUTE_KEY = AttributeKey.stringKey("metricTimesliceName");

    private final Map<String, Attributes> timesliceAttributesCache = new ConcurrentHashMap<>();

    private final DoubleHistogram timesliceHistogram;
    private final DoubleCounter timesliceCounter;

    private OpenTelemetryMetricsAggregator(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter(SCOPE_NAME);
        this.timesliceHistogram = meter.histogramBuilder(NEWRELIC_TIMESLICE_HISTOGRAM_VALUE_METRIC).build();
        this.timesliceCounter = meter.counterBuilder(NEWRELIC_TIMESLICE_COUNTER_VALUE_METRIC).ofDoubles().build();
    }

    static OpenTelemetryMetricsAggregator create(OpenTelemetry openTelemetry) {
        return new OpenTelemetryMetricsAggregator(openTelemetry);
    }

    @Override
    public void recordMetric(String name, float value) {
        timesliceHistogram.record(value, attributeForTimeslice(name));
    }

    @Override
    public void recordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
        timesliceHistogram.record(convertToSeconds(totalTime, timeUnit), attributeForTimeslice(name));
    }

    @Override
    public void recordResponseTimeMetric(String name, long millis) {
        timesliceHistogram.record(convertToSeconds(millis, TimeUnit.MILLISECONDS), attributeForTimeslice(name));
    }

    private static double convertToSeconds(long timeValue, TimeUnit timeUnit) {
        long nanos = timeUnit.toNanos(timeValue);
        return nanos / NANOS_PER_SECOND;
    }

    @Override
    public void incrementCounter(String name) {
        timesliceCounter.add(1, attributeForTimeslice(name));
    }

    @Override
    public void incrementCounter(String name, int count) {
        timesliceCounter.add(count, attributeForTimeslice(name));
    }

    private Attributes attributeForTimeslice(String timesliceName) {
        return timesliceAttributesCache.computeIfAbsent(timesliceName, unused -> Attributes.of(NEWRELIC_TIMESLICE_ATTRIBUTE_KEY, timesliceName));
    }

}
