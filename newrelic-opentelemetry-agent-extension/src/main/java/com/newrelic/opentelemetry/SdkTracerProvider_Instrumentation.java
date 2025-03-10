///*
// *
// *  * Copyright 2024 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package com.newrelic.opentelemetry;
//
//import com.newrelic.api.agent.Config;
//import com.newrelic.api.agent.NewRelic;
//import com.newrelic.api.agent.weaver.MatchType;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.Weaver;
//import io.opentelemetry.api.trace.TracerBuilder;
//import io.opentelemetry.sdk.trace.TracerSharedState;
//
///**
// * Weaved to inject a New Relic Java agent implementation of an OpenTelemetry TracerBuilder
// */
//@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.sdk.trace.SdkTracerProvider")
//public final class SdkTracerProvider_Instrumentation {
//    private final TracerSharedState sharedState = Weaver.callOriginal();
//
//    public TracerBuilder tracerBuilder(String instrumentationScopeName) {
//        final TracerBuilder tracerBuilder = Weaver.callOriginal();
//        Config config = NewRelic.getAgent().getConfig();
//        if (NRSpanBuilder.isSpanBuilderEnabled(config)) {
//            // return our tracer builder instead of the OTel instance
//            return new NRTracerBuilder(config, instrumentationScopeName, sharedState);
//        }
//        return tracerBuilder;
//    }
//}
