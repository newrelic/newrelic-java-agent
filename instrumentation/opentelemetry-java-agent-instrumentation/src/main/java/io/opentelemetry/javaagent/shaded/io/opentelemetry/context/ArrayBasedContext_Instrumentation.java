//package io.opentelemetry.javaagent.shaded.io.opentelemetry.context;
//
//import com.newrelic.api.agent.weaver.MatchType;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.WeaveAllConstructors;
//import io.opentelemetry.sdk.trace.Helper;
//
//@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.javaagent.shaded.io.opentelemetry.context.ArrayBasedContext")
//class ArrayBasedContext_Instrumentation {
//
//    @WeaveAllConstructors
//    ArrayBasedContext_Instrumentation() {
//        Helper.foo("ArrayBasedContext_Instrumentation");
//    }
//
//}
