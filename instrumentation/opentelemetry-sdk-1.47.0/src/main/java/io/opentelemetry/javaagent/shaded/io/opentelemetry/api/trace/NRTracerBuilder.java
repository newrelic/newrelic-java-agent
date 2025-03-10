///*
// *
// *  * Copyright 2025 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace;
//
//import com.newrelic.agent.bridge.AgentBridge;
//import com.newrelic.api.agent.Config;
//import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.OpenTelemetry;
//import io.opentelemetry.sdk.trace.TracerSharedStateWrapper;
//
///**
// * New Relic Java agent implementation of an OpenTelemetry
// * TracerBuilder, which is a factory for building OpenTelemetry Tracers.
// * An OpenTelemetry Tracer can then be used to create OpenTelemetry Spans.
// */
//public class NRTracerBuilder implements TracerBuilder {
//    private final String instrumentationScopeName;
//    private final TracerSharedStateWrapper tracerSharedStateWrapper;
//    private final Config config;
//    private String schemaUrl;
//    private String instrumentationScopeVersion;
//
//    public NRTracerBuilder(Config config, String instrumentationScopeName, TracerSharedStateWrapper tracerSharedStateWrapper) {
//        this.config = config;
//        this.instrumentationScopeName = instrumentationScopeName;
//        this.tracerSharedStateWrapper = tracerSharedStateWrapper;
//    }
//
//    @Override
//    public TracerBuilder setSchemaUrl(String schemaUrl) {
//        this.schemaUrl = schemaUrl;
//        return this;
//    }
//
//    @Override
//    public TracerBuilder setInstrumentationVersion(String instrumentationScopeVersion) {
//        this.instrumentationScopeVersion = instrumentationScopeVersion;
//        return this;
//    }
//
//    @Override
//    public Tracer build() {
//        Boolean enabled = config.getValue("opentelemetry.instrumentation." + instrumentationScopeName + ".enabled");
//        if (enabled != null && !enabled) {
//            return OpenTelemetry.noop().getTracer(instrumentationScopeName);
//        } else {
//            return spanName -> new NRSpanBuilder(AgentBridge.instrumentation, instrumentationScopeName, instrumentationScopeVersion, tracerSharedStateWrapper, spanName);
//        }
//    }
//}
