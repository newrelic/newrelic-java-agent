package com.newrelic.opentelemetry;

import com.newrelic.api.agent.metrics.Counter;
import com.newrelic.api.agent.metrics.Meter;
import com.newrelic.api.agent.metrics.Summary;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.newrelic.opentelemetry.OpenTelemetryNewRelic.toAttributes;

public class OpenTelemetryMeter implements Meter {
    private final io.opentelemetry.api.metrics.Meter meter;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Summary> summaries = new ConcurrentHashMap<>();

    private OpenTelemetryMeter(OpenTelemetry openTelemetry) {
        meter = openTelemetry.getMeter("newrelic-java-agent");
    }

    public static OpenTelemetryMeter create(OpenTelemetry openTelemetry) {
        return new OpenTelemetryMeter(openTelemetry);
    }

    @Override
    public Counter newCounter(String name) {
        return counters.computeIfAbsent(name, key -> {
            final LongCounter longCounter = meter.counterBuilder(key).build();
            return (increment, attributes) -> longCounter.add(increment, toAttributes(attributes).build());
        });
    }

    @Override
    public Summary newSummary(String name) {
        return summaries.computeIfAbsent(name, key -> {
            final DoubleHistogram histogram = meter.histogramBuilder(key).build();
            return (value, attributes) -> histogram.record(value, toAttributes(attributes).build());
        });
    }
}
