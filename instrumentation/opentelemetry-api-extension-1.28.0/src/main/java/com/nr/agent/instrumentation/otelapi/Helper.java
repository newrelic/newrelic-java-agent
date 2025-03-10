package com.nr.agent.instrumentation.otelapi;

public class Helper {

    public static void foo(String s) {
        System.out.println("this was called from " + s);
    }

}
