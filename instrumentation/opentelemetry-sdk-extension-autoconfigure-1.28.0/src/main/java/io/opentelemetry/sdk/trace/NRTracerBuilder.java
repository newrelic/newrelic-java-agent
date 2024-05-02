package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.AgentBridge;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;

class NRTracerBuilder implements TracerBuilder {
    private String schemaUrl;
    private String instrumentationScopeVersion;

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
        return spanName -> new NRSpanBuilder(AgentBridge.instrumentation, schemaUrl, instrumentationScopeVersion, spanName);
    }
}
