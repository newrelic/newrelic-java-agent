/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.AgentBridge;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;

import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.isOpenTelemetryTracerDisabled;

/**
 * New Relic Java agent implementation of an OpenTelemetry
 * TracerBuilder, which is a factory for building OpenTelemetry Tracers.
 * An OpenTelemetry Tracer can then be used to create OpenTelemetry Spans.
 */
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

    /**
     * Builds a new OpenTelemetry Tracer.
     * If the OTel Tracer is disabled in the configuration, returns a noop Tracer.
     */
    @Override
    public Tracer build() {
        if (isOpenTelemetryTracerDisabled(instrumentationScopeName)) {
            return OpenTelemetry.noop().getTracer(instrumentationScopeName);
        } else {
            return spanName -> new NRSpanBuilder(AgentBridge.instrumentation, instrumentationScopeName, instrumentationScopeVersion, sharedState, spanName);
        }
    }
}
