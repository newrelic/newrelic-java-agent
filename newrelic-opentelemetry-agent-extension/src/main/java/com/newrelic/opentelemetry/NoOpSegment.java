package com.newrelic.opentelemetry;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;

import java.util.Map;

final class NoOpSegment implements Segment {

    private static final NoOpSegment INSTANCE = new NoOpSegment();

    private NoOpSegment() {
    }

    static NoOpSegment getInstance() {
        return INSTANCE;
    }

    @Override
    public void addCustomAttribute(String key, Number value) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "addCustomAttribute");
    }

    @Override
    public void addCustomAttribute(String key, String value) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "addCustomAttribute");
    }

    @Override
    public void addCustomAttribute(String key, boolean value) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "addCustomAttribute");
    }

    @Override
    public void addCustomAttributes(Map<String, Object> attributes) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "addCustomAttributes");
    }

    @Override
    public void setMetricName(String... metricNameParts) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "setMetricName");
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "reportAsExternal");
    }

    @Override
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "addOutboundRequestHeaders");
    }

    @Override
    public Transaction getTransaction() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "getTransaction");
        return OpenTelemetryTransaction.getInstance();
    }

    @Override
    public void ignore() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "ignore");
    }

    @Override
    public void end() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "end");
    }

    @Override
    public void endAsync() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Segment", "endAsync");
    }
}
