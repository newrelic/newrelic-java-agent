///*
// *
// *  * Copyright 2025 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package io.opentelemetry.sdk.trace;
//
//import io.opentelemetry.sdk.resources.Resource;
//
//public class TracerSharedStateWrapper {
//    private final TracerSharedState tracerSharedState;
//
//    TracerSharedStateWrapper(TracerSharedState tracerSharedState) {
//        this.tracerSharedState = tracerSharedState;
//    }
//    public TracerSharedState getTracerSharedState() {
//        return tracerSharedState;
//    }
//
//    public SpanProcessor getActiveSpanProcessor() {
//        return tracerSharedState.getActiveSpanProcessor();
//    }
//
//    public Resource getResource() {
//        return tracerSharedState.getResource();
//    }
//
//}
