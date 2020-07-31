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

public class AsyncKeysTest extends AsyncTest {

    @Test(timeout = 120000)
    public void testThreadAsContext() throws InterruptedException {
        MyChildThread child = new MyChildThread();
        MyInitialThread initial = new MyInitialThread(child, child);
        initial.start();
        // just joining on the parent is fine b/c parent waits for child to finish
        initial.join();

        verifyMetrics(initial, child);
    }

    @Test(timeout = 120000)
    public void testStringAsContext() throws InterruptedException {
        runTest("1234567");
    }

    @Test(timeout = 120000)
    public void testLongAsContext() throws InterruptedException {
        runTest(123456789L);
    }

    @Test(timeout = 120000)
    public void testIntAsContext() throws InterruptedException {
        runTest(1111111);
    }

    @Test(timeout = 120000)
    public void testArrayAsContext() throws InterruptedException {
        runTest(new int[] { 1, 2, 3, 4 });
    }

    @Test(timeout = 120000)
    public void testMapAsContext() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("123", 11L);
        values.put("45", new Object());
        runTest(values);
    }

    @Test(timeout = 120000)
    public void testRandomObjectAsContext1() throws InterruptedException {
        runTest(new ObjectWithBoolean());
    }

    @Test(timeout = 120000)
    public void testRandomObjectAsContext2() throws InterruptedException {
        runTest(new ObjectWithConstantHash());
    }

    @Test(timeout = 120000)
    public void testRandomObjectAsContext3() throws InterruptedException {
        runTest(new ObjectWithInt(11));
    }

    class ObjectWithBoolean {
        private boolean hello;

        public void setHello(boolean hello) {
            this.hello = hello;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + (hello ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ObjectWithBoolean other = (ObjectWithBoolean) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (hello != other.hello) {
                return false;
            }
            return true;
        }

        private AsyncKeysTest getOuterType() {
            return AsyncKeysTest.this;
        }

    }

    class ObjectWithConstantHash {
        private boolean hello;

        public void setHello(boolean hello) {
            this.hello = hello;
        }

        @Override
        public int hashCode() {
            return 31;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ObjectWithConstantHash other = (ObjectWithConstantHash) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (hello != other.hello) {
                return false;
            }
            return true;
        }

        private AsyncKeysTest getOuterType() {
            return AsyncKeysTest.this;
        }

    }

    class ObjectWithInt {
        private int hello;

        public ObjectWithInt(int val) {
            hello = val;
        }

        public void setHello(int hello) {
            this.hello = hello;
        }

        @Override
        public int hashCode() {
            return hello;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ObjectWithInt other = (ObjectWithInt) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (hello != other.hello) {
                return false;
            }
            return true;
        }

        private AsyncKeysTest getOuterType() {
            return AsyncKeysTest.this;
        }

    }

    public void runTest(Object context) throws InterruptedException {
        MyChildThread child = new MyChildThread(context);
        MyInitialThread initial = new MyInitialThread(context, child);
        initial.start();
        // just joining on the parent is fine b/c parent waits for child to finish
        initial.join();

        verifyMetrics(initial, child);
    }

    private void verifyMetrics(Thread initial, Thread child) {
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.AsyncKeysTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.AsyncKeysTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.AsyncKeysTest$MyChildThread/run");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.AsyncKeysTest$MyInitialThread/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.AsyncKeysTest$MyInitialThread/run");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.AsyncKeysTest$MyInitialThread/run",
                "Java/com.newrelic.agent.async.AsyncKeysTest$MyInitialThread/run", initial.getName(),
                "Java/com.newrelic.agent.async.AsyncKeysTest$MyChildThread/run", child.getName());
        verifyNoExceptions();
    }

    class MyInitialThread extends Thread {
        Object context;
        Thread thread;

        public MyInitialThread(Object asyncContext, Thread toStart) {
            context = asyncContext;
            thread = toStart;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {

            AgentBridge.getAgent().getTransaction().registerAsyncActivity(context);
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

        public MyChildThread() {
            context = this;
        }

        public MyChildThread(Object pContext) {
            context = pContext;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            AgentBridge.getAgent().startAsyncActivity(context);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }
}
