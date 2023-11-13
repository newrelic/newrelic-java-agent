package com.newrelic.opentelemetry;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.TraceMetadata;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import io.opentelemetry.api.OpenTelemetry;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Note this and {@link #create(OpenTelemetry)} are public because they are
 * accessed from package {@code com.newrelic.api.agent} after
 * {@link com.newrelic.api.agent.NewRelic} is rewritten.
 */
public final class OpenTelemetryAgent implements Agent {

    private final OpenTelemetryMetricsAggregator openTelemetryMetricsAggregator;
    private final OpenTelemetryInsights openTelemetryInsights;

    private OpenTelemetryAgent(OpenTelemetry openTelemetry) {
        this.openTelemetryMetricsAggregator = OpenTelemetryMetricsAggregator.create(openTelemetry);
        this.openTelemetryInsights = OpenTelemetryInsights.create(openTelemetry);
    }

    public static OpenTelemetryAgent create(OpenTelemetry openTelemetry) {
        Objects.requireNonNull(openTelemetry, "openTelemetry must not be null");
        return new OpenTelemetryAgent(openTelemetry);
    }

    @Override
    public TracedMethod getTracedMethod() {
        return OpenTelemetryTracedMethod.getInstance();
    }

    @Override
    public Transaction getTransaction() {
        return OpenTelemetryTransaction.getInstance();
    }

    @Override
    public Logger getLogger() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Agent", "getLogger");
        return NoOpLogger.getInstance();
    }

    @Override
    public Config getConfig() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Agent", "getConfig");
        return NoOpConfig.getInstance();
    }

    @Override
    public MetricAggregator getMetricAggregator() {
        return openTelemetryMetricsAggregator;
    }

    @Override
    public Insights getInsights() {
        return openTelemetryInsights;
    }

    @Override
    public TraceMetadata getTraceMetadata() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Agent", "getTraceMetadata");
        return NoOpTraceMetadata.getInstance();
    }

    @Override
    public Map<String, String> getLinkingMetadata() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Agent", "getLinkingMetadata");
        return Collections.emptyMap();
    }

    public OpenTelemetryErrorApi getErrorApi() {
        return OpenTelemetryErrorApi.getInstance();
    }

}
