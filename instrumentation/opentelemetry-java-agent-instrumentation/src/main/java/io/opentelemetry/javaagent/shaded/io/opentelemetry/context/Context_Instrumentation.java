/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.javaagent.shaded.io.opentelemetry.context;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.header.utils.LogThis;
//import io.opentelemetry.sdk.trace.Helper;

//import io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context;
//import io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Scope;

@Weave(type = MatchType.Interface, originalName = "io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context")
public abstract class Context_Instrumentation {

//    private static final String CONTEXT_CLASS = "io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context";
//    private static final String SCOPE_CLASS = "io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Scope";

//    static {
//        try {
//            Class.forName(CONTEXT_CLASS);
//            Class.forName(SCOPE_CLASS);
//            LogThis.foo("static initializer");
//        } catch (final ClassNotFoundException e) {
//            LogThis.foo("static initializer exception");
//        }
//    }
    /**
     * The purpose of this initializer is to hook into a place that's only called once during initialization of the
     * spray-http library so we can work around an issue where our agent fails to transform the RequestContext class.
     */
//    static {
//        AgentBridge.getAgent().getLogger().log(Level.FINER, "Retransforming io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context");
//        AgentBridge.instrumentation.retransformUninstrumentedClass(Context.class);
//        AgentBridge.getAgent().getLogger().log(Level.FINER, "Retransformed io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context");
//    }


    public static Context current() {
//        Helper.foo("current");
        LogThis.foo("current");
//        return ContextHelper.current(Weaver.callOriginal());
        return Weaver.callOriginal();
    }

    public Scope makeCurrent() {
//        Helper.foo("makeCurrent");
        LogThis.foo("makeCurrent");
//        return ContextHelper.makeCurrent((Context) this, Weaver.callOriginal());
        return Weaver.callOriginal();
    }
}
