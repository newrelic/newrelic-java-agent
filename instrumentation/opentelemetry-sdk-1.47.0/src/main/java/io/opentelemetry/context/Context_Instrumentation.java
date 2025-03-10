///*
// *
// *  * Copyright 2025 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package io.opentelemetry.context;
//
//import com.newrelic.api.agent.weaver.MatchType;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.Weaver;
//import io.opentelemetry.sdk.trace.Helper;
//
//@Weave(type = MatchType.Interface, originalName = "io.opentelemetry.context.Context")
//public abstract class Context_Instrumentation {
//
//    public static Context current() {
//        Helper.foo("current");
//        return ContextHelper.current(Weaver.callOriginal());
//    }
//
//    public Scope makeCurrent() {
//        Helper.foo("makeCurrent");
//        return ContextHelper.makeCurrent((Context) this, Weaver.callOriginal());
//    }
//}
