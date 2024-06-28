package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;

class NRTracerBuilder implements TracerBuilder {
    private final String instrumentationScopeName;
    private final TracerSharedState sharedState;
    private final Config config;
    private String schemaUrl;
    private String instrumentationScopeVersion;

    public NRTracerBuilder(Config config, String instrumentationScopeName, TracerSharedState sharedState) {
        this.config = config;
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
        Boolean enabled = config.getValue("opentelemetry.instrumentation." + instrumentationScopeName + ".enabled");
        if (enabled != null && !enabled) {
            return OpenTelemetry.noop().getTracer(instrumentationScopeName);
        } else {
            return spanName -> new NRSpanBuilder(AgentBridge.instrumentation, instrumentationScopeName, instrumentationScopeVersion, sharedState, spanName);
        }
    }
}