package io.opentelemetry.sdk.trace;

import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.TracerBuilder;

//import io.opentelemetry.api.trace.TracerBuilder;

public class Helper {

    public static void foo(String s) {
        System.out.println("this was called from " + s);
    }

//    public static void clazz(Object o) {
//        System.out.println("this was called from " + o);
//    }

//    public static void SdkTracerProvider(SdkTracerProvider_Instrumentation sdkTracerProviderInstrumentation) {
//        System.out.println("this was called from " + sdkTracerProviderInstrumentation);
//    }

//    public static void TracerBuilder(TracerBuilder tracerBuilder) {
//        System.out.println("this was called from " + tracerBuilder);
//    }

//    public static void TracerSharedState(TracerSharedState sharedState) {
//        System.out.println("this was called from " + sharedState);
//    }

//    public static void TracerSharedState(Object sharedState) {
//        System.out.println("this was called from " + sharedState);
//    }

//    public static void tracerSharedState(TracerSharedState tracerSharedState) {
//        System.out.println("this was called from " + tracerSharedState);
//    }

}
