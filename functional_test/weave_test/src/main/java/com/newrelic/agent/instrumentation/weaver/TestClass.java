/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class TestClass {
	public boolean preambleRan;
	public boolean postambleRan;
	public boolean exceptionLogged;

    // a variable on the original class we want to access
    private int privateVariableAccessTest;

    // for constructor testing
    public String constructorReturnString;

    public TestClass() {
        constructorReturnString = "weaved " + constructorReturnString;
    }

    public TestClass(int constructorReturnNum) {
        constructorReturnString = "weaved " + constructorReturnString;
    }

    @Trace
    public void foo() {

        NewRelic.noticeError("Test Error");
        Weaver.callOriginal();

        System.err.println("After foo called");
    }

    public String getPath(IRequest request) {
        String orig = Weaver.callOriginal();
        return "/oh" + orig + request.getPath();
    }

    public Callable<String> getCallable() {
        final Callable<String> callable = Weaver.callOriginal();
        return new Callable<String>() {

            @Override
            public String call() throws Exception {
                return "wrap " + callable.call();
            }
        };
    }

    public void apiCallAfter() {
        Weaver.callOriginal();
        NewRelic.noticeError("Error after");
    }

    public Object wrappedCallOriginal(Object obj) throws Exception {
        Object result;
        try {
            result = Weaver.callOriginal();
        } catch (Exception t) {
            NewRelic.noticeError(t);
            throw t;
        }
        return result;
    }

    public void throwsException() throws Exception {
        try {
            Weaver.callOriginal();
        } catch (Exception t) {
            NewRelic.noticeError(t);
            throw t;
        }
    }

    public Object throwsException(Object obj) throws Exception {
        Object result;
        try {
            result = Weaver.callOriginal();
        } catch (Exception t) {
            NewRelic.noticeError(t);
            throw t;
        }
        return result;
    }

    public String integerTest() {
        int fooBar = 7;

        String orig = Weaver.callOriginal();
        return orig + fooBar;
    }

    public String longTest() {
        long fooBar = 8;

        String orig = Weaver.callOriginal();
        return orig + fooBar;
    }

    public String floatTest() {
        float fooBar = 9;

        String orig = Weaver.callOriginal();
        return orig + fooBar;
    }

    public String doubleTest() {
        double fooBar = 10;

        String orig = Weaver.callOriginal();
        return orig + fooBar;
    }

    public String charTest() {
        char fooBar = '5';

        String orig = Weaver.callOriginal();
        return orig + fooBar;
    }

    public String stringTest() {
        String fooBar = "yo";

        String orig = Weaver.callOriginal();
        return orig + fooBar;
    }

    public String arrayTest() {
        String[] fooBar = { "yo" };

        String orig = Weaver.callOriginal();
        return orig + fooBar.length;
    }

    public String booleanTest() {
        boolean fooBar = true;

        String orig = Weaver.callOriginal();
        return orig + fooBar;
    }

    public String discardOriginalReturn() {
        Weaver.callOriginal();
        return "dude";
    }

    public static int staticMethod(int x) {
        return x + Weaver.<Integer> callOriginal();
    }

    public double discardOriginalReturn(double x) {
        Weaver.callOriginal();
        return 55d;
    }

    public String byteTest() {
        byte fooBar = 123;

        String orig = Weaver.callOriginal();
        return orig + fooBar;
    }

    public double callInlineMethod() {
        return Weaver.<Double> callOriginal() + inline();
    }

    public double callInlineAfter(int x) {
        Weaver.callOriginal();
        return inline();
    }

    /**
     * This method will get inlined.
     */
    private double inline() {
        // show that we can call out to a helper class that will get loaded into the correct classloader.
        return DummyHelper.oneOhOne();
    }

    public String instrumentationErrorBefore() {

        Object t = null;
        t.toString();

        return Weaver.callOriginal();
    }

    public String testExistingInnerClass() {
        Weaver.callOriginal();

        return new InnerClass().test() + "_hmm";
    }

    public String instrumentationErrorAfter() {

        String val = Weaver.callOriginal();

        Object t = null;
        t.toString();

        return val;
    }

    public int getPrivateVariableAccessTest() {
        Weaver.callOriginal();
        return privateVariableAccessTest;
    }

    public int changeArgs(int i) {
        i = 666;
        return Weaver.callOriginal();
    }

    public int overrideReturn() {
        Weaver.callOriginal();
        return fooBar();
    }

    public void errorTrapExitTest() {
    	preambleRan = true;
    	String s = null;
    	s.toString();
    	Weaver.callOriginal();
    	postambleRan = true;
    }

    public String checkedExceptionTest() throws Exception {
        try {
            return Weaver.callOriginal();
        } catch (Exception e) {
            exceptionLogged = true;
            throw e;
        }
    }

    public String checkedExceptionTest2() throws Exception {
        try {
            return Weaver.callOriginal();
        } catch (Exception e) {
            exceptionLogged = true;
            throw e;
        }
    }

    public String checkedThrowableTest() throws Exception {
        try {
            return Weaver.callOriginal();
        } catch (Exception e) {
            exceptionLogged = true;
            throw e;
        }
    }

    public String checkedExceptionRuntimeTest() throws Exception {
        try {
            return Weaver.callOriginal();
        } catch (Exception e) {
            exceptionLogged = true;
            throw e;
        }
    }

    public String checkedRuntimeExceptionTest() throws Exception {
        try {
            return Weaver.callOriginal();
        } catch (Exception e) {
            exceptionLogged = true;
            throw e;
        }
    }

    public String uncheckedRuntimeExceptionTest() throws Exception {
        try {
            return Weaver.callOriginal();
        } catch (Exception e) {
            exceptionLogged = true;
            throw e;
        }
    }

    public String uncheckedRuntimeExceptionTest2() throws Exception {
        try {
            return Weaver.callOriginal();
        } catch (Exception e) {
            exceptionLogged = true;
            throw e;
        }
    }

    public void getTokenApiSupportabilityNPEBefore() {
        Token token = AgentBridge.getAgent().getTransaction(false).getToken();
        token.linkAndExpire();
        Weaver.callOriginal();
    }

    public void getTokenApiSupportabilityNPEAfter() {
        Weaver.callOriginal();
        Token token = AgentBridge.getAgent().getTransaction(false).getToken();
        token.linkAndExpire();
    }

    @Trace
    private int fooBar(){
    	return Weaver.callOriginal();
    }

    @Weave
    public static class InnerClass {
        public String test() {
            Weaver.callOriginal();
            return "Weave";
        }
    }
}
