/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class ApiCallingAsyncTest extends AsyncTest {

    @Test
    public void testStartCalledWithoutRegister() throws InterruptedException {
        String context = "123";
        MyChildThread child = new MyChildThread(context, true);
        MyInitialThread root = new MyInitialThread(context, child, false);
        root.start();
        root.join();
        child.join();

        // will have two separate transactions - initial thread should always finish last
        verifyTimesSet(2);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run");
        verifyScopedMetricsNotPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run", root.getName());
        verifyNoExceptions();
    }

    @Test
    public void testRegisterCalledWithoutStart() throws InterruptedException {
        String context = "123";
        MyChildThread child = new MyChildThread(context, false);
        MyInitialThread root = new MyInitialThread(context, child, true);
        root.start();
        root.join();
        child.join();

        // child tx should finish - initial tx should be waiting for start call
        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run");
        verifyScopedMetricsNotPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run");

        child = new MyChildThread(context, true);
        child.start();
        child.join();
        verifyTimesSet(2);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run");

    }

    @Test
    public void testRegisterCalledOutsideAnyTransaction() throws InterruptedException {
        String context = "123";
        MyChildThread child = new MyChildThread(context, true);
        Assert.assertFalse(AgentBridge.getAgent().getTransaction().registerAsyncActivity(context));
        MyInitialThread root = new MyInitialThread(context, child, false);
        root.start();
        root.join();
        child.join();

        // will have two separate transactions - initial thread should always finish last
        verifyTimesSet(2);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run");
        verifyScopedMetricsNotPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run", root.getName());
        verifyNoExceptions();

    }

    @Test
    public void testStartCalledOutsideAnyTransaction1() throws InterruptedException {
        String context = "123";
        MyChildThread child = new MyChildThread(context, false);
        // should do nothing
        Assert.assertFalse(AgentBridge.getAgent().startAsyncActivity(context));
        MyInitialThread root = new MyInitialThread(context, child, false);
        root.start();
        root.join();
        child.join();

        // will have two separate transactions - initial thread should always finish last
        verifyTimesSet(2);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run");
        verifyScopedMetricsNotPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run", root.getName());
        verifyNoExceptions();
    }

    @Test
    public void testStartCalledOutsideAnyTransaction2() throws InterruptedException {
        String context = "123";
        MyChildThread child = new MyChildThread(context, false);
        // should do nothing
        MyInitialThread root = new MyInitialThread(context, child, true);
        Assert.assertFalse(AgentBridge.getAgent().startAsyncActivity(context));
        root.start();
        root.join();
        child.join();

        // child tx should finish - initial tx should be waiting for start call
        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run");
        verifyScopedMetricsNotPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run");

        child = new MyChildThread(context, true);
        child.start();
        child.join();
        verifyTimesSet(2);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$MyChildThread/run");
    }

    class MyInitialThread extends Thread {
        Object context;
        Thread thread;
        boolean register;

        public MyInitialThread(Object asyncContext, Thread toStart, boolean shouldRegister) {
            context = asyncContext;
            thread = toStart;
            register = shouldRegister;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {

            if (register) {
                Assert.assertTrue(AgentBridge.getAgent().getTransaction().registerAsyncActivity(context));
            }
            thread.start();
            try {
                thread.join();
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }

        }
    }

    class MyChildThread extends Thread {
        private final Object context;
        private final boolean start;

        public MyChildThread(boolean callStart) {
            context = this;
            start = callStart;
        }

        public MyChildThread(Object pContext, boolean callStart) {
            context = pContext;
            start = callStart;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            if (start) {
                Assert.assertTrue(AgentBridge.getAgent().startAsyncActivity(context));
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

    @Test
    public void testStartAsyncTwice() throws InterruptedException {
        final class Child extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().startAsyncActivity(this));
                Assert.assertFalse(AgentBridge.getAgent().startAsyncActivity(this));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        final Child child = new Child();

        final class Parent extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().getTransaction().registerAsyncActivity(child));
                child.start();
                try {
                    child.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        final Parent parent = new Parent();
        parent.start();
        parent.join();

        verifyScopedMetricsPresent("OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child/run");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent/run");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent/run", parent.getName(),
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child/run", child.getName());
        verifyNoExceptions();
    }

    volatile boolean c1result;
    volatile boolean c2result;

    @Test
    public void testStartAsyncTwiceSeparateThreads() throws InterruptedException {
        final String key = "4567";

        final class Child1 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                c1result = AgentBridge.getAgent().startAsyncActivity(key);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        final class Child2 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                c2result = AgentBridge.getAgent().startAsyncActivity(key);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        final Child1 child1 = new Child1();
        final Child2 child2 = new Child2();

        final class Parent1 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().getTransaction().registerAsyncActivity(key));
                child1.start();
                child2.start();
                try {
                    child1.join();
                    child2.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Parent1 parent = new Parent1();
        parent.start();
        parent.join();

        boolean b1 = c1result | c2result;
        boolean b2 = c1result & c2result;
        Assert.assertTrue(b1); // one call to startAsyncActivity must have succeeded
        Assert.assertFalse(b2); // ...but they had better not both have succeeded.

        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent1/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent1/run");

        String winningChildMetric;
        String losingChildMetric;
        String winningChildName;

        if (c1result) { // child1 was the one that succeeded on the start call, child2 didn't
            winningChildMetric = "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child1/run";
            losingChildMetric = "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child2/run";
            winningChildName = child1.getName();
        } else { // vice versa
            winningChildMetric = "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child2/run";
            losingChildMetric = "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child1/run";
            winningChildName = child2.getName();
        }

        verifyScopedMetricsPresent("OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent1/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent1/run", winningChildMetric);
        verifyScopedMetricsNotPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent1/run", losingChildMetric);
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent1/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent1/run", parent.getName(), winningChildMetric,
                winningChildName);

        verifyNoExceptions();
    }

    @Test
    public void testRegisterTwoChildren() throws InterruptedException {
        final String key1 = "45678";
        final String key2 = "9999";
        final class Child11 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().startAsyncActivity(key1));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        final class Child22 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().startAsyncActivity(key2));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        final Child11 child1 = new Child11();
        final Child22 child2 = new Child22();

        final class Parent11 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().getTransaction().registerAsyncActivity(key1));
                Assert.assertTrue(AgentBridge.getAgent().getTransaction().registerAsyncActivity(key2));
                child1.start();
                child2.start();
                try {
                    child1.join();
                    child2.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Parent11 parent = new Parent11();
        parent.start();
        parent.join();

        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent11/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent11/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child11/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child22/run");

        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent11/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent11/run");
        verifyTransactionSegmentsChildren("Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent11/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child11/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child22/run");
        verifyNoExceptions();
    }

    @Test
    public void testRegisterNotInParent() throws InterruptedException {
        final String key1 = "45678";
        final String key2 = "9999";
        final class Child111 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().startAsyncActivity(key1));
                Assert.assertTrue(AgentBridge.getAgent().getTransaction().registerAsyncActivity(key2));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        final class Child222 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().startAsyncActivity(key2));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        final Child111 child1 = new Child111();
        final Child222 child2 = new Child222();

        final class Parent111 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().getTransaction().registerAsyncActivity(key1));
                child1.start();
                try {
                    child1.join();
                    child2.start();
                    child2.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Parent111 parent = new Parent111();
        parent.start();
        parent.join();

        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent111/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent111/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child111/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child222/run");

        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent111/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent111/run");
        verifyTransactionSegmentsChildren("Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Parent111/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child111/run");
        verifyTransactionSegmentsChildren("Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child111/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1Child222/run");
        verifyNoExceptions();
    }

    @Test
    public void teststartChildInNonRoot() throws InterruptedException {
        final String key1 = "45678";
        final class ChildWithMethods extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                method1();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Trace
            private void method1() {
                method2();
            }

            @Trace
            private void method2() {
                Assert.assertTrue(AgentBridge.getAgent().startAsyncActivity(key1));
            }
        }
        final ChildWithMethods child1 = new ChildWithMethods();

        final class TheParent extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Assert.assertTrue(AgentBridge.getAgent().getTransaction().registerAsyncActivity(key1));
                child1.start();
                try {
                    child1.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        TheParent parent = new TheParent();
        parent.start();
        parent.join();

        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1TheParent/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1TheParent/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1ChildWithMethods/run");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1TheParent/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1TheParent/run");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1TheParent/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1TheParent/run", parent.getName(),
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1ChildWithMethods/run", child1.getName(),
                "Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1ChildWithMethods/method1", NO_ASYNC_CONTEXT,
                "Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1ChildWithMethods/method2", NO_ASYNC_CONTEXT);
        verifyNoExceptions();
    }

    @Test
    public void testregisterChildInNonRoot() throws InterruptedException {
        final String key1 = "45678";
        final class ChildWithMethods2 extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                method1();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Trace
            private void method1() {
                method2();
            }

            @Trace
            private void method2() {
                AgentBridge.getAgent().startAsyncActivity(key1);
            }
        }
        final ChildWithMethods2 child1 = new ChildWithMethods2();

        final class ParentWithMethods extends Thread {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                methoda();
                child1.start();
                try {
                    child1.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Trace
            public void methoda() {
                methodb();
            }

            @Trace
            public void methodb() {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(key1);
            }
        }

        ParentWithMethods parent = new ParentWithMethods();
        parent.start();
        parent.join();

        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1ParentWithMethods/run",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1ParentWithMethods/run",
                "Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1ParentWithMethods/methoda",
                "Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1ParentWithMethods/methodb",
                "Java/com.newrelic.agent.async.ApiCallingAsyncTest$1ChildWithMethods2/run",
                "Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1ChildWithMethods2/method1",
                "Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1ChildWithMethods2/method2");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1ParentWithMethods/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.ApiCallingAsyncTest$1ParentWithMethods/run");
        verifyNoExceptions();
    }

    @Test(timeout = 10000)
    public void testNaming1() throws Exception {
        final Request req = new Request() {

            @Override
            public HeaderType getHeaderType() {
                return null;
            }

            @Override
            public String getHeader(String name) {
                return null;
            }

            @Override
            public String getRequestURI() {
                return null;
            }

            @Override
            public String getRemoteUser() {
                return null;
            }

            @Override
            public Enumeration<?> getParameterNames() {
                return null;
            }

            @Override
            public String[] getParameterValues(String name) {
                return null;
            }

            @Override
            public Object getAttribute(String name) {
                return null;
            }

            @Override
            public String getCookieValue(String name) {
                return null;
            }

        };

        final Response resp = new Response() {

            @Override
            public HeaderType getHeaderType() {
                return null;
            }

            @Override
            public void setHeader(String name, String value) {

            }

            @Override
            public int getStatus() throws Exception {
                return 0;
            }

            @Override
            public String getStatusMessage() throws Exception {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

        };

        // The two threads call APIs in different orders. The purpose of the test is to ensure
        // that the same metrics get reported either way.

        Thread t1 = new Thread() {
            @Override
            @Trace(dispatcher = true)
            public void run() {
                NewRelic.setTransactionName(null, "MyOtherTransaction");
                NewRelic.setRequestAndResponse(req, resp);
            }
        };

        t1.start();
        t1.join();
        TransactionStats s1 = getStats();

        Thread t2 = new Thread() {
            @Override
            @Trace(dispatcher = true)
            public void run() {
                NewRelic.setRequestAndResponse(req, resp);
                NewRelic.setTransactionName(null, "MyOtherTransaction");
            }
        };

        t2.start();
        t2.join();
        TransactionStats s2 = getStats();

        assertSameKeys(s1, s2, new String[] { "Java/com.newrelic.agent.async.ApiCallingAsyncTest\\$\\d/run" });
    }

    static class T1 extends Thread {
        List<Thread> threads = new ArrayList<>(com.newrelic.api.agent.TransactionNamePriority.values().length);

        @Override
        @Trace(dispatcher = true)
        public void run() {
            for (com.newrelic.api.agent.TransactionNamePriority p : com.newrelic.api.agent.TransactionNamePriority.values()) {
                Thread t = new T2(p);
                threads.add(t);
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(t);
                t.start();
            }

            while (threads.size() > 0) {
                Iterator<Thread> iter = threads.iterator();
                while (iter.hasNext()) {
                    Thread t = iter.next();
                    try {
                        t.join();
                        iter.remove();
                    } catch (InterruptedException e) {
                        ; // ignore and catch on next iteration of while
                    }
                }
            }
        }
    }

    static class T2 extends Thread {
        com.newrelic.api.agent.TransactionNamePriority tnp;

        public T2(com.newrelic.api.agent.TransactionNamePriority tnp) {
            this.tnp = tnp;
        }

        @Override
        @Trace(dispatcher = true)
        public void run() {
            AgentBridge.getAgent().startAsyncActivity(this);
            Thread.currentThread().setName("test-thread-" + tnp.toString());
            long end = System.nanoTime() + 2L * 1000L * 1000L * 1000L;
            while (System.nanoTime() < end) {
                NewRelic.getAgent().getTransaction().setTransactionName(tnp, true, "CATEGORY", tnp.toString());
            }
        }
    }

    @Test(timeout = 10000)
    public void testNaming2() throws Exception {
        Thread t1 = new T1();
        t1.start();
        t1.join();
        int n = com.newrelic.api.agent.TransactionNamePriority.values().length;
        // dumpData();
        // System.out.println("NAME: " + this.getData().getPriorityTransactionName().getName());
        Assert.assertTrue(this.getData().getPriorityTransactionName().getName().equals(
                "OtherTransaction/" + "CATEGORY/" + com.newrelic.api.agent.TransactionNamePriority.values()[n - 1]));
    }
}
