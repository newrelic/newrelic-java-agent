///*
// *
// *  * Copyright 2025 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace;
//
//import com.newrelic.api.agent.weaver.MatchType;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.Weaver;
//import io.opentelemetry.sdk.trace.Helper;
//
//@Weave(type = MatchType.Interface, originalName = "io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.TracerProvider")
//public abstract class TracerProvider_Instrumentation {
//
//    public TracerBuilder tracerBuilder(String instrumentationScopeName) {
////        Helper.foo("tracerBuilder");
//        TracerBuilder tracerBuilder = Weaver.callOriginal();
////        if (tracerBuilder instanceof SdkTracerBuilder) {
////            ((SdkTracerBuilder) tracerBuilder)
////        }
//        Helper.TracerBuilder(tracerBuilder);
//        return tracerBuilder;
//    }
//
//
//}
