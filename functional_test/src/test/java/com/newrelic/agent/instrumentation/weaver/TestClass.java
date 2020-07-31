/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import java.io.IOException;
import java.util.concurrent.Callable;

public class TestClass {
    public boolean preambleRan = false;
    public boolean postambleRan = false;
    public boolean exceptionLogged = false;

    private int privateVariableAccessTest = 999;

    public String constructorReturnString = "unknown";

    public TestClass() {
    }

    public TestClass(int constructorReturnNum) {
        if (constructorReturnNum <= 0) {
            constructorReturnString = "too few";
            return;
        }

        if (constructorReturnNum == 1) {
            constructorReturnString = "one";
            return;
        }

        if (constructorReturnNum == 2) {
            constructorReturnString = "two";
            return;
        }

        constructorReturnString = "too many";
    }

    public void foo() {
        System.err.println("foo");
    }

    public String getPath(IRequest request) {
        return request.getPath();
    }

    public int bar(int i) {
        return 777 + fooBar();
    }

    private int fooBar() {
        return 777;
    }

    public int overrideReturn() {
        return 9090;
    }

    public String integerTest() {
        return "Dude";
    }

    public String doubleTest() {
        return "Dude";
    }

    public String longTest() {
        return "Dude";
    }

    public String floatTest() {
        return "Dude";
    }

    public String charTest() {
        return "Dude";
    }

    public String stringTest() {
        return "Dude";
    }

    public String arrayTest() {
        return "Dude";
    }

    public String booleanTest() {
        return "Dude";
    }

    public String byteTest() {
        return "Dude";
    }

    public double callInlineMethod() {
        return 808;
    }

    public double callInlineAfter(int x) {
        return 808 + x;
    }

    public int changeArgs(int i) {
        return i;
    }

    public void apiCallAfter() {
        // no op
    }

    public static int staticMethod(int x) {
        return x;
    }

    public String discardOriginalReturn() {
        return "Test";
    }

    public double discardOriginalReturn(double x) {
        return x;
    }

    public String testExistingInnerClass() {
        return new InnerClass().test();
    }

    public Object wrappedCallOriginal(Object obj) {
        return obj.toString();
    }

    public void throwsException() {
        throw new RuntimeException("Yo");
    }

    public Object throwsException(Object t) {
        t.toString();
        throw new RuntimeException("Dude");
    }

    public String instrumentationErrorBefore() {
        changeArgs(5);
        return "Before";
    }

    public String checkedExceptionTest() throws Exception {
        throw new UnsupportedOperationException();
    }

    public String checkedExceptionTest2() throws IOException {
        throw new IOException();
    }

    public String checkedThrowableTest() throws Throwable {
        throw new OutOfMemoryError();
    }

    public String checkedExceptionRuntimeTest() throws Exception {
        throw new OutOfMemoryError();
    }

    public String checkedRuntimeExceptionTest() throws RuntimeException {
        throw new IllegalStateException();
    }

    public String uncheckedRuntimeExceptionTest() {
        throw new RuntimeException();
    }

    public String uncheckedRuntimeExceptionTest2() throws UnsupportedOperationException {
        throw new RuntimeException();
    }

    public String instrumentationErrorAfter() {
        return "After";
    }

    public int getPrivateVariableAccessTest() {
        return 0;
    }

    public void errorTrapExitTest() {
    }

    public void getTokenApiSupportabilityNPEBefore() {
    }

    public void getTokenApiSupportabilityNPEAfter() {
    }

    public Callable<String> getCallable() {
        return new Callable<String>() {

            @Override
            public String call() throws Exception {
                return "callable";
            }

        };
    }

    public static class InnerClass {
        public String test() {
            return "original";
        }
    }
}
