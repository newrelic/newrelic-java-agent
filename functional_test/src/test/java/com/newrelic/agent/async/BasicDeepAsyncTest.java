/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class BasicDeepAsyncTest extends AsyncTest {

    @Test
    public void testTwoThreadWithTraces() throws InterruptedException {
        MyChildThread child = new MyChildThread();
        RootThread initial = new RootThread(child);

        initial.start();
        // just joining on the parent is fine b/c parent waits for child to finish
        initial.join();

        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThread/run",
                "Java/com.newrelic.agent.async.BasicDeepAsyncTest$RootThread/run",
                "Java/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/run",
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/methoda",
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/methodb");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThread/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThread/run");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThread/run",
                "Java/com.newrelic.agent.async.BasicDeepAsyncTest$RootThread/run", initial.getName(),
                "Java/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/run", child.getName(),
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/methoda", NO_ASYNC_CONTEXT,
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/methodb", NO_ASYNC_CONTEXT);
        verifyNoExceptions();
    }

    @Test
    public void testTwoThreadmultiChildren() throws InterruptedException {
        MyChildThread child = new MyChildThread();
        RootThreadWithLocalChildren initial = new RootThreadWithLocalChildren(child);

        initial.start();
        // just joining on the parent is fine b/c parent waits for child to finish
        initial.join();

        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/run",
                "Java/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/run",
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/method1",
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/method2",
                "Java/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/run",
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/methoda",
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/methodb");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/run");
        Map<String, String> expected = new HashMap<>();
        expected.put("Java/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/run",
                initial.getName());
        expected.put("Java/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/run", child.getName());
        verifyTransactionSegmentNodesWithExecContext(expected);
        verifyTransactionSegmentsChildren(
                "Java/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/run",
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/method1",
                "Custom/com.newrelic.agent.async.BasicDeepAsyncTest$RootThreadWithLocalChildren/method2",
                "Java/com.newrelic.agent.async.BasicDeepAsyncTest$MyChildThread/run");
        verifyNoExceptions();
    }

    class RootThread extends Thread {
        private final Thread child;

        public RootThread(Thread childThread) {
            child = childThread;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {

            AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
            child.start();

            try {
                child.join();
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    class RootThreadWithLocalChildren extends Thread {
        private final Thread child;

        public RootThreadWithLocalChildren(Thread childThread) {
            child = childThread;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {

            AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
            method1();
            method2();
            child.start();

            try {
                child.join();
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        @Trace
        private void method1() {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }

        @Trace
        private void method2() {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

    class MyChildThread extends Thread {

        @Trace(dispatcher = true)
        @Override
        public void run() {

            AgentBridge.getAgent().startAsyncActivity(this);
            methoda();
            methodb();
        }

        @Trace
        private void methoda() {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }

        @Trace
        private void methodb() {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

}
