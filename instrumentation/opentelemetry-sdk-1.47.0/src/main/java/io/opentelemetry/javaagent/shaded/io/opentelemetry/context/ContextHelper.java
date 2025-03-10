///*
// *
// *  * Copyright 2025 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package io.opentelemetry.javaagent.shaded.io.opentelemetry.context;
//
//import com.newrelic.agent.bridge.AgentBridge;
//import com.newrelic.agent.bridge.ExitTracer;
//import com.newrelic.agent.bridge.Transaction;
//import com.newrelic.api.agent.TracedMethod;
////import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.ExitTracerSpan;
////import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.Span;
//
///**
// * Helper class for managing the OpenTelemetry Context
// */
//class ContextHelper {
//    private ContextHelper() {
//    }
//
//    /**
//     * If there's no span on the context, but there is a NR tracer on the stack, return a context with our span.
//     */
//    public static Context current(Context context) {
//        Span currentSpan = Span.fromContext(context);
//        if (currentSpan == Span.getInvalid()) {
//            Transaction transaction = AgentBridge.getAgent().getTransaction(false);
//            if (transaction != null) {
//                TracedMethod tracedMethod = transaction.getTracedMethod();
//                if (tracedMethod instanceof ExitTracer) {
//                    return context.with(ExitTracerSpan.wrap((ExitTracer) tracedMethod));
//                }
//            }
//        }
//        return context;
//    }
//
//    /**
//     * If there's currently no NR transaction but the current contains a NR span, create a
//     * {@link com.newrelic.api.agent.Token} related to that span's transaction and hook it into
//     * the returned {@link io.opentelemetry.context.Scope}.
//     */
//    public static Scope makeCurrent(Context context, Scope scope) {
//        final Transaction currentTransaction = AgentBridge.getAgent().getTransaction(false);
//        if (currentTransaction == null) {
//            Span currentSpan = Span.fromContext(context);
//
//            if (currentSpan instanceof ExitTracerSpan) {
//                return ((ExitTracerSpan) currentSpan).createScope(scope);
//            }
//        }
//        return scope;
//    }
//}
