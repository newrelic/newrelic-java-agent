/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Assert;
import org.junit.Test;

import java.net.ConnectException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class BasicAsyncApiTest extends AsyncTest {

    @Test
    public void apiCallAfterChildTxa() throws InterruptedException {

        final class ChildThread extends Thread {
            boolean error = false;
            Exception ex = null;

            @Override
            public void run() {
                try {
                    work();
                    makeApiCalls();
                    work1();
                } catch (Exception e) {
                    error = true;
                    ex = e;
                    e.printStackTrace();
                }
            }

            @Trace(dispatcher = true)
            public void work() {
                AgentBridge.getAgent().startAsyncActivity(this);
            }

            @Trace
            public void work1() {

            }

            public boolean getError() {
                return error;
            }
        }

        final ChildThread child = new ChildThread();

        final Thread parent = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                try {
                    AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
                    child.start();
                    child.join();

                } catch (InterruptedException e) {
                }
            }
        };

        parent.start();
        parent.join();

        Assert.assertFalse("Error should not have been thrown when accessing the API. Exception: " + child.ex,
                child.getError());
    }

    private void makeApiCalls() {
        NewRelic.incrementCounter("fooParent");
        NewRelic.incrementCounter("barParent", 10);
        NewRelic.addCustomParameter("one", 3);
        NewRelic.setTransactionName("hi", "bye");
        NewRelic.getAgent().getMetricAggregator().incrementCounter("foobar");
        NewRelic.getAgent().getTracedMethod().setMetricName("one", "two", "three");
        // these throw null pointers because getLastTracer and getTracedMethod return null
        // NewRelic.getAgent().getTransaction().getLastTracer().getMetricName();
        // NewRelic.getAgent().getTransaction().getTracedMethod().setMetricName("three", "four", "five");
        NewRelic.getAgent().getTransaction().getLastTracer();
        NewRelic.getAgent().getTransaction().getTracedMethod();
        AgentBridge.getAgent().getTransaction().registerAsyncActivity("123");
        AgentBridge.getAgent().startAsyncActivity("123");
    }

    @Test
    public void apiCallAfterTransaction() throws InterruptedException {

        final class ChildThread extends Thread {
            boolean error = false;
            Exception ex;

            @Trace(dispatcher = true)
            @Override
            public void run() {
                try {
                    work();
                    makeApiCalls();
                    work1();
                } catch (Exception e) {
                    error = true;
                    ex = e;
                }
            }

            public void work() {
                AgentBridge.getAgent().startAsyncActivity(this);
            }

            @Trace
            public void work1() {

            }

            public boolean getError() {
                return error;
            }
        }

        final ChildThread child = new ChildThread();

        final class ParentThread extends Thread {
            boolean error = false;
            Exception ex;

            @Override
            public void run() {
                try {
                    work();
                    makeApiCalls();
                } catch (Exception e) {
                    error = true;
                    ex = e;
                }
            }

            @Trace(dispatcher = true)
            public void work() throws InterruptedException {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
                child.start();
                child.join();
            }

            public boolean getError() {
                return error;
            }
        }

        final ParentThread parent = new ParentThread();

        parent.start();
        parent.join();

        Assert.assertFalse("Error should not have been thrown when accessing the API. Exception: " + child.ex,
                child.getError());
        Assert.assertFalse("Error should not have been thrown when accessing the API. Exception: " + parent.ex,
                parent.getError());
    }

    // waits for the second thread to finish before finishing itself.
    // a starts then b starts, then b finishes, then a finishes
    @Test(timeout = 120000)
    public void testTwoThreads() throws InterruptedException {
        MyInitialThreadWaitsForChild initial = new MyInitialThreadWaitsForChild();
        initial.start();
        // just joining on the parent is fine b/c parent waits for child to finish
        initial.join();

        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadWaitsForChild/run",
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadWaitsForChild/run",
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyChildThread/run");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadWaitsForChild/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadWaitsForChild/run");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadWaitsForChild/run",
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadWaitsForChild/run", initial.threadName,
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyChildThread/run", initial.childThreadName);
        verifyNoExceptions();
    }

    class MyInitialThreadWaitsForChild extends Thread {

        String threadName;
        String childThreadName;

        @Trace(dispatcher = true)
        @Override
        public void run() {

            String context = "123456";
            MyChildThread thread = new MyChildThread(context);
            threadName = Thread.currentThread().getName();
            AgentBridge.getAgent().getTransaction().registerAsyncActivity(context);
            // call again just for kicks, should do nothing:
            AgentBridge.getAgent().getTransaction().registerAsyncActivity(context);
            thread.start();
            try {
                thread.join();
                Thread.sleep(1);
                childThreadName = thread.threadName;
            } catch (InterruptedException e) {
                // ignore
            }

        }
    }

    class MyChildThread extends Thread {
        private final Object context;
        private String threadName;

        public MyChildThread() {
            context = this;
        }

        public MyChildThread(String pContext) {
            context = pContext;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            AgentBridge.getAgent().startAsyncActivity(context);
            threadName = Thread.currentThread().getName();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

    // first thread exists before second thread is complete
    @Test(timeout = 1200000)
    public void testTwoThreadsInitialExitsImmediately() throws InterruptedException {
        MyInitialThreadExitsImmediately initial = new MyInitialThreadExitsImmediately();

        initial.start();
        // join on parent and child as child should finish last
        initial.join();
        initial.getChild().join();

        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadExitsImmediately/run",
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadExitsImmediately/run",
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyChildThreadWaits/run");
        verifyUnscopedMetricsPresent(
                "OtherTransaction/all",
                "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadExitsImmediately/run",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadExitsImmediately/run");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadExitsImmediately/run",
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialThreadExitsImmediately/run",
                initial.threadName, "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyChildThreadWaits/run",
                initial.getChild().childThreadName);
        verifyNoExceptions();

    }

    class MyInitialThreadExitsImmediately extends Thread {

        private final MyChildThreadWaits child;
        String threadName;

        public MyInitialThreadExitsImmediately() {
            child = new MyChildThreadWaits(this);
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            threadName = Thread.currentThread().getName();
            AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
            child.start();
            // finish immediately
        }

        public MyChildThreadWaits getChild() {
            return child;
        }
    }

    class MyChildThreadWaits extends Thread {

        private final Thread initialThread;
        String childThreadName;

        public MyChildThreadWaits(Thread initThread) {
            initialThread = initThread;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            AgentBridge.getAgent().startAsyncActivity(this);
            childThreadName = Thread.currentThread().getName();
            try {
                // wait for parent to finish first
                initialThread.join();
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

    // Start the second thread after the first thread has completed.
    @Test(timeout = 120000)
    public void testSecondsStartsAfterFirstFinishes() throws InterruptedException, ExecutionException {

        ExecutorService service = Executors.newFixedThreadPool(1);
        MyInitialCallable initial = new MyInitialCallable();
        Future<MyChildThread> task = service.submit(initial);
        MyChildThread childThread = task.get();
        childThread.start();
        childThread.join();

        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialCallable/call",
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialCallable/call",
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyChildThread/run");
        verifyUnscopedMetricsPresent("OtherTransaction/all", "OtherTransactionTotalTime",
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialCallable/call",
                "OtherTransactionTotalTime/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialCallable/call");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialCallable/call",
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyInitialCallable/call", initial.threadName,
                "Java/com.newrelic.agent.async.BasicAsyncApiTest$MyChildThread/run", childThread.threadName);
        verifyNoExceptions();
    }

    class MyInitialCallable implements Callable<MyChildThread> {
        String threadName;

        @Trace(dispatcher = true)
        @Override
        public MyChildThread call() throws Exception {
            threadName = Thread.currentThread().getName();
            MyChildThread thread = new MyChildThread();
            AgentBridge.getAgent().getTransaction().registerAsyncActivity(thread);
            return thread;
        }
    }

    // This test shows how migrate handles same-priority transaction and application names that have been
    // set in both the parent and the child. Since the startAsyncActivity() call occurs after the parent
    // sets these values, the child is considered to write last and therefore "best".
    @Test(timeout = 120000)
    public void testTransactionMigrate() throws Exception {
        final Thread child = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Transaction.getTransaction().setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE,
                        "Child App Name");
                NewRelic.setTransactionName("CATEGORY", "ChildTxName");
                NewRelic.addCustomParameter("ChildCustomParam", "FromChild");

                AgentBridge.getAgent().startAsyncActivity(this);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        final Thread parent = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Transaction.getTransaction().setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE,
                        "Parent App Name");
                NewRelic.setTransactionName("CATEGORY", "ParentTxName");
                NewRelic.addCustomParameter("ParentCustomParam", "FromParent");
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
                child.start();
                try {
                    child.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        parent.start();
        parent.join();

        waitForTransaction();
        Assert.assertEquals("Child App Name", data.getApplicationName());
        Assert.assertEquals("OtherTransaction/CATEGORY/ChildTxName", data.getPriorityTransactionName().getName());
        Assert.assertEquals(2, data.getUserAttributes().size());
        Assert.assertEquals("FromChild", data.getUserAttributes().get("ChildCustomParam"));
        Assert.assertEquals("FromParent", data.getUserAttributes().get("ParentCustomParam"));
    }

    // This is like testTransactionMigrate() except the priorities of child's setApplicationName and of
    // parent's setTransactionName have been altered so both names give the opposite answer as the test
    // above after the startAsync (migrate) occurs.
    @Test(timeout = 120000)
    public void testTransactionMigrate2() throws Exception {
        final Thread child = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Transaction.getTransaction().setApplicationName(ApplicationNamePriority.CONTEXT_PARAM, "Child App Name");
                NewRelic.getAgent().getTransaction().setTransactionName(
                        com.newrelic.api.agent.TransactionNamePriority.FRAMEWORK_LOW, true, "CHILD_CATEGORY", "ChildTxName");
                NewRelic.addCustomParameter("ChildCustomParam", "FromChild");

                AgentBridge.getAgent().startAsyncActivity(this);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        final Thread parent = new Thread() {
            @Trace(dispatcher = true)
            @Override
            public void run() {
                Transaction.getTransaction().setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE,
                        "Parent App Name");
                NewRelic.setTransactionName("PARENT_CATEGORY", "ParentTxName");
                NewRelic.addCustomParameter("ParentCustomParam", "FromParent");
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
                child.start();
                try {
                    child.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        parent.start();
        parent.join();

        waitForTransaction();
        Assert.assertEquals("Parent App Name", data.getApplicationName());
        Assert.assertEquals("OtherTransaction/PARENT_CATEGORY/ParentTxName", data.getPriorityTransactionName().getName());
        Assert.assertEquals(2, data.getUserAttributes().size());
        Assert.assertEquals("FromChild", data.getUserAttributes().get("ChildCustomParam"));
        Assert.assertEquals("FromParent", data.getUserAttributes().get("ParentCustomParam"));
    }

    @Test(timeout = 120000)
    public void testAsyncExternalCall() throws Throwable {
        final AtomicReference<String> guid = new AtomicReference<>();
        final ExceptionCatchingThread child = new ExceptionCatchingThread() {
            @Trace(dispatcher = true)
            @Override
            public void runUnderCatch() {
                Transaction t1 = Transaction.getTransaction();
                AgentBridge.getAgent().startAsyncActivity(this);
                Assert.assertNotEquals(t1, Transaction.getTransaction());

                // For purposes of this test, it doesn't matter whether there is anything listening
                // on port 80 or not; the point is that the call gets made. Otoh because it's a test,
                // we don't want to catch and ignore just any exception, either. So if the test fails
                // because of an exception in the code below, have to look at the exception and decide
                // whether it's a problem or we just need an additional catch clause.

                org.apache.http.client.HttpClient httpClient = new DefaultHttpClient();
                HttpUriRequest httpget = new HttpGet("http://localhost");
                try {
                    httpClient.execute(httpget);
                } catch (ConnectException e) {
                    // Ignore if nothing running on localhost:80
                } catch (Exception e) {
                    Assert.fail("Unexpected exception during open-ended connect attempt: " + e);
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }
            }
        };

        final ExceptionCatchingThread parent = new ExceptionCatchingThread() {
            @Trace(dispatcher = true)
            @Override
            public void runUnderCatch() {
                AgentBridge.getAgent().getTransaction().registerAsyncActivity(child);
                child.start();
                try {
                    child.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Assert.assertNotNull(Transaction.getTransaction().getCrossProcessTransactionState().getTripId());
            }
        };

        parent.start();
        parent.join();
        child.join();
        child.checkThrow();
        parent.checkThrow();

        waitForTransaction();
        Assert.assertNotNull(data.getGuid());
        Assert.assertNotNull(data.getTripId());
    }

    public abstract class ExceptionCatchingThread extends Thread {
        public volatile Throwable ex;

        abstract void runUnderCatch() throws Exception;

        @Override
        public void run() {
            try {
                runUnderCatch();
            } catch (Throwable t) {
                ex = t;
                throw new Error(ex);
            }
        }

        public void checkThrow() throws Throwable {
            if (ex != null) {
                throw ex;
            }
        }
    }

    private void waitForTransaction() {
        long start = System.currentTimeMillis();
        // Wait for data to be available
        while ((System.currentTimeMillis() - start) < 5000 && (data == null || stats == null)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }
}
