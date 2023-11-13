package com.newrelic.opentelemetry;

import com.newrelic.api.agent.DistributedTracePayload;

final class NoOpDistributedTracePayload implements DistributedTracePayload {

    private static final NoOpDistributedTracePayload INSTANCE = new NoOpDistributedTracePayload();

    private NoOpDistributedTracePayload() {
    }

    static NoOpDistributedTracePayload getInstance() {
        return INSTANCE;
    }

    @Override
    public String text() {
        OpenTelemetryNewRelic.logUnsupportedMethod("DistributedPayload", "text");
        return null;
    }

    @Override
    public String httpSafe() {
        OpenTelemetryNewRelic.logUnsupportedMethod("DistributedPayload", "httpSafe");
        return null;
    }
}
