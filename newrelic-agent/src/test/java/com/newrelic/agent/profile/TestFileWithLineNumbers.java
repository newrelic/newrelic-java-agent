/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestFileWithLineNumbers {
    private final StackTraceElement[] trace;

    public TestFileWithLineNumbers() {
        trace = Thread.currentThread().getStackTrace();
    }

    public TestFileWithLineNumbers(int one, long two) {
        trace = Thread.currentThread().getStackTrace();
    }

    public TestFileWithLineNumbers(int[] one, byte[][] two, String[][][] three) {
        trace = Thread.currentThread().getStackTrace();
    }

    public TestFileWithLineNumbers(Object one, List<String> two, String three, Set<Object> four) {
        trace = Thread.currentThread().getStackTrace();
    }

    public StackTraceElement[] getTrace() {
        return trace;
    }

    public StackTraceElement[] foo() {
        return Thread.currentThread().getStackTrace();
    }

    public StackTraceElement[] foo(String one) {
        return Thread.currentThread().getStackTrace();
    }

    public StackTraceElement[] foo(String one, String two) {
        return Thread.currentThread().getStackTrace();
    }

    public StackTraceElement[] foo(int one, int two) {
        return Thread.currentThread().getStackTrace();
    }

    public StackTraceElement[] foo(long one, long two) {
        return Thread.currentThread().getStackTrace();
    }

    public StackTraceElement[] foo(Integer one, Integer two) {
        return Thread.currentThread().getStackTrace();
    }

    public StackTraceElement[] foo(short one, byte two, int[] three, List<String> four, Map<String, String> five) {
        return Thread.currentThread().getStackTrace();
    }

    public StackTraceElement[] foo(Integer[][] one, Object[] two) {
        return Thread.currentThread().getStackTrace();
    }

    @InstrumentedMethod(instrumentationTypes = InstrumentationType.TraceAnnotation, instrumentationNames = "theName")
    public void instrumentedMethod() { }
}
