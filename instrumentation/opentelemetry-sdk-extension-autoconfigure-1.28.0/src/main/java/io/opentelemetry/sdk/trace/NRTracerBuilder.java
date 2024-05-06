package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.AgentBridge;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;

class NRTracerBuilder implements TracerBuilder {
    private final String instrumentationScopeName;
    private final TracerSharedState sharedState;
    private String schemaUrl;
    private String instrumentationScopeVersion;

    public NRTracerBuilder(String instrumentationScopeName, TracerSharedState sharedState) {
        this.instrumentationScopeName = instrumentationScopeName;
        this.sharedState = sharedState;
    }

    @Override
    public TracerBuilder setSchemaUrl(String schemaUrl) {
        this.schemaUrl = schemaUrl;
        return this;
    }

    @Override
    public TracerBuilder setInstrumentationVersion(String instrumentationScopeVersion) {
        this.instrumentationScopeVersion = instrumentationScopeVersion;
        return this;
    }

    @Override
    public Tracer build() {
        return spanName -> new NRSpanBuilder(AgentBridge.instrumentation, instrumentationScopeName, instrumentationScopeVersion, sharedState, spanName);
    }
}
