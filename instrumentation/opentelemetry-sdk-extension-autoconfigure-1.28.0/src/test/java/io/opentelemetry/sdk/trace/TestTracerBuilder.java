/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTracerBuilder implements TracerBuilder {
    private final String instrumentationScopeName;
    private String instrumentationScopeVersion;
    private final Instrumentation instrumentation = mock(Instrumentation.class);
    private final List<SpanProcessor> spanProcessors = new ArrayList<>();
    private Resource resource = Resource.empty();

    public TestTracerBuilder(String instrumentationScopeName) {
        this.instrumentationScopeName = instrumentationScopeName;
    }

    public TestTracerBuilder addSpanProcessor(SpanProcessor processor) {
        this.spanProcessors.add(processor);
        return this;
    }

    public TestTracerBuilder setResource(Resource resource) {
        this.resource = resource;
        return this;
    }

    @Override
    public TracerBuilder setSchemaUrl(String schemaUrl) {
        return this;
    }

    @Override
    public TracerBuilder setInstrumentationVersion(String instrumentationScopeVersion) {
        return this;
    }

    public TracerBuilder withTracer(ExitTracer tracer) {
        when(instrumentation.createTracer(anyString(), anyInt())).thenReturn(tracer);
        return this;
    }

    @Override
    public Tracer build() {
        Supplier<SpanLimits> spanLimitsSupplier = () -> SpanLimits.getDefault();
        TracerSharedState sharedState = new TracerSharedState(Clock.getDefault(), IdGenerator.random(), resource, spanLimitsSupplier, Sampler.alwaysOn(),
                spanProcessors);
        return spanName -> new NRSpanBuilder(instrumentation, instrumentationScopeName, instrumentationScopeVersion, sharedState, spanName);
    }
}
