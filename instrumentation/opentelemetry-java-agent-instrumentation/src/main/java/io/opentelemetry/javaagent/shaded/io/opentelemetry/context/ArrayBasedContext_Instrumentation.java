//package io.opentelemetry.javaagent.shaded.io.opentelemetry.context;
//
//import com.newrelic.api.agent.weaver.MatchType;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.WeaveAllConstructors;
//import com.newrelic.api.agent.weaver.Weaver;
//import com.nr.agent.instrumentation.header.utils.LogThis;
//import io.opentelemetry.sdk.trace.Helper;
//
//@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.javaagent.shaded.io.opentelemetry.context.ArrayBasedContext")
//class ArrayBasedContext_Instrumentation {
//
//    public static Context current() {
////        Helper.foo("current");
//        LogThis.foo("current");
////        return ContextHelper.current(Weaver.callOriginal());
//        return Weaver.callOriginal();
//    }
//
//    public Scope makeCurrent() {
////        Helper.foo("makeCurrent");
//        LogThis.foo("makeCurrent");
////        return ContextHelper.makeCurrent((Context) this, Weaver.callOriginal());
//        return Weaver.callOriginal();
//    }
//}
