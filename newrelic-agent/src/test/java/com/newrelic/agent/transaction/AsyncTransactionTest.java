/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionAsyncUtility;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.test.marker.RequiresFork;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category(RequiresFork.class)
public class AsyncTransactionTest implements TransactionListener {

    private TransactionData data;
    private TransactionStats stats;

    @BeforeClass
    public static void beforeClass() throws Exception {
        TransactionAsyncUtility.createServiceManager(createConfigMap());
    }

    @Before
    public void before() throws Exception {
        data = null;
        stats = null;
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @After
    public void after() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    private static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "Unit Test");
        map.put("token_timeout", 12000);
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return map;
    }

    @Test
    public void testRegisterTwice() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer("one");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        final String context1 = "123";
        Assert.assertTrue(ServiceFactory.getAsyncTxService().registerAsyncActivity(context1));
        Assert.assertFalse(ServiceFactory.getAsyncTxService().registerAsyncActivity(context1));
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        Thread a1 = new Thread() {
            @Override
            public void run() {
                Transaction.clearTransaction();
                Transaction oldTx = Transaction.getTransaction();
                Tracer rootTracer = createDispatcherTracer("one" + context1.toString());
                oldTx.getTransactionActivity().tracerStarted(rootTracer);

                Assert.assertTrue(ServiceFactory.getAsyncTxService().startAsyncActivity(context1));
                Transaction newTx = Transaction.getTransaction();
                Assert.assertTrue(oldTx != newTx);

                newTx.getTransactionActivity().tracerFinished(rootTracer, 0);
            }
        };
        a1.start();
        a1.join();

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, tx.getFinishedChildren().size());
    }

    @Test
    public void testAsyncFinishesOnce() throws Throwable {
        for (int i = 0; i < 100; ++i) {
            Transaction.clearTransaction();
            Transaction tx = Transaction.getTransaction();
            Tracer rootTracer = createDispatcherTracer("one");
            tx.getTransactionActivity().tracerStarted(rootTracer);
            final String context1 = "123";
            Assert.assertTrue(ServiceFactory.getAsyncTxService().registerAsyncActivity(context1));
            Assert.assertNull(data);
            Assert.assertNull(stats);
            final long sleepTime = 1;

            Thread a1 = new Thread() {
                @Override
                public void run() {
                    Transaction.clearTransaction();
                    Transaction oldTx = Transaction.getTransaction();
                    Tracer rootTracer = createDispatcherTracer("one" + context1.toString());
                    oldTx.getTransactionActivity().tracerStarted(rootTracer);

                    Assert.assertTrue(ServiceFactory.getAsyncTxService().startAsyncActivity(context1));
                    Assert.assertNull(ServiceFactory.getAsyncTxService().extractIfPresent(context1));
                    Transaction newTx = Transaction.getTransaction();
                    Assert.assertTrue(oldTx != newTx);

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    newTx.getTransactionActivity().tracerFinished(rootTracer, 0);
                }
            };
            long startTs = System.currentTimeMillis();
            a1.start();
            tx.getTransactionActivity().tracerFinished(rootTracer, 0); // txa1 is done
            for (int j = 0; j < 1000; ++j) {
                if ((System.currentTimeMillis() - startTs) >= sleepTime) {
                    // sleep time is over. txa2 has finished so the tx should have finished
                    break;
                }
                // txa2 is sleeping. So the transaction should not be finished
                try {
                    Assert.assertFalse(tx.isFinished());
                } catch (Throwable t) {
                    if ((System.currentTimeMillis() - startTs) < sleepTime) {
                        // sleep time is not over. should not have finished.
                        throw t;
                    }
                }
            }
            try {
                a1.join();
            } catch (InterruptedException e) {
                Assert.fail(e.getMessage());
            }
            waitForTransaction();
            Assert.assertEquals(2, tx.getFinishedChildren().size());
            Assert.assertTrue(tx.isFinished());
            data = null;
            stats = null;
        }
    }

    @Test
    public void testStartNormalOne() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer("one");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        String context1 = "123";
        ServiceFactory.getAsyncTxService().registerAsyncActivity(context1);
        rootTracer.finish(Opcodes.RETURN, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        Activity a1 = new Activity(context1);
        a1.start();
        a1.join();

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, tx.getFinishedChildren().size());
    }

    @Test
    public void testStartNormalTwo() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer("one");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        String context1 = "123";
        ServiceFactory.getAsyncTxService().registerAsyncActivity(context1);
        Long context2 = 1234567L;
        ServiceFactory.getAsyncTxService().registerAsyncActivity(context2);
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        Activity a1 = new Activity(context1);
        a1.start();
        a1.join();

        Assert.assertNull(data);
        Assert.assertNull(stats);

        Activity a2 = new Activity(context2);
        a2.start();
        a2.join();

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(3, tx.getFinishedChildren().size());
    }

    @Test
    public void testStartWithoutRegister() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer("one");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        String context1 = "123";
        // NO register

        Assert.assertNull(data);
        Assert.assertNull(stats);

        Activity a1 = new Activity(context1);
        a1.start();
        a1.join();

        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(1, tx.getFinishedChildren().size());
    }

    class Activity extends Thread {
        Object context;

        public Activity(Object context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                Transaction.clearTransaction();
                Transaction tx = Transaction.getTransaction();
                Tracer rootTracer = createDispatcherTracer("one" + context.toString());
                tx.getTransactionActivity().tracerStarted(rootTracer);
                ServiceFactory.getAsyncTxService().startAsyncActivity(context);
                tx.getTransactionActivity().tracerFinished(rootTracer, 0);

            } catch (Exception e) {
                Assert.fail("An exception should not have been thrown: " + e.getMessage());
            }
        }
    }

    @Test
    public void testOnlyFirstRegisterOnKeyWins() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer("hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        String context1 = "12345678";
        ServiceFactory.getAsyncTxService().registerAsyncActivity(context1);
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        ActivitySameKey activity1 = new ActivitySameKey(context1);
        activity1.start();
        activity1.join();

        Activity activity2 = new Activity(context1);
        activity2.start();
        activity2.join();

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, tx.getFinishedChildren().size());
        List<Tracer> tracers = tx.getTracers();
        boolean foundExpectedName = false;
        for (Tracer current : tracers) {
            Assert.assertFalse(current.getTransactionSegmentName().contains("shouldnotBePresent" + context1));
            if (current.getTransactionSegmentName().contains("one" + context1)) {
                foundExpectedName = true;
            }
        }
        Assert.assertTrue("There was no transaction segment which started with one", foundExpectedName);
    }

    class ActivitySameKey extends Thread {
        Object context;

        public ActivitySameKey(Object context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                Transaction.clearTransaction();
                Transaction tx = Transaction.getTransaction();
                Tracer rootTracer = createDispatcherTracer("shouldnotBePresent" + context.toString());
                tx.getTransactionActivity().tracerStarted(rootTracer);
                ServiceFactory.getAsyncTxService().registerAsyncActivity(context);
                tx.getTransactionActivity().tracerFinished(rootTracer, 0);

            } catch (Exception e) {
                Assert.fail("An exception should not have been thrown: " + e.getMessage());
            }
        }
    }

    @Test
    public void testStartTwice() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer("hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        String context1 = "12345678";
        ServiceFactory.getAsyncTxService().registerAsyncActivity(context1);
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        ActivityStartTwice activity1 = new ActivityStartTwice(context1);
        activity1.start();
        activity1.join();

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, tx.getFinishedChildren().size());
        List<Tracer> tracers = tx.getTracers();
        boolean foundExpectedName = false;
        for (Tracer current : tracers) {
            if (current.getTransactionSegmentName().contains("startTwice" + context1)) {
                foundExpectedName = true;
            }
        }
        Assert.assertTrue("There was no transaction segment which started with startTwice", foundExpectedName);
    }

    class ActivityStartTwice extends Thread {
        Object context;

        public ActivityStartTwice(Object context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                Transaction.clearTransaction();
                Transaction tx = Transaction.getTransaction();
                Tracer rootTracer = createDispatcherTracer("startTwice" + context.toString());
                tx.getTransactionActivity().tracerStarted(rootTracer);
                ServiceFactory.getAsyncTxService().startAsyncActivity(context);
                ServiceFactory.getAsyncTxService().startAsyncActivity(context);
                tx.getTransactionActivity().tracerFinished(rootTracer, 0);

            } catch (Exception e) {
                Assert.fail("An exception should not have been thrown: " + e.getMessage());
            }
        }
    }

    @Test
    public void testActivityStartOutsideTransaction1() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer("hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        String context1 = "12345678900";
        ServiceFactory.getAsyncTxService().registerAsyncActivity(context1);
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        ActivityStartOutsideTransaction activity1 = new ActivityStartOutsideTransaction(context1, true);
        activity1.start();
        activity1.join();

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(1, tx.getFinishedChildren().size());
        List<Tracer> tracers = tx.getTracers();
        for (Tracer current : tracers) {
            Assert.assertFalse(current.getTransactionSegmentName().contains("outsideTrans" + context1));
        }
    }

    @Test
    public void testActivityStartOutsideTransaction2() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer("hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        String context1 = "123456789010";
        ServiceFactory.getAsyncTxService().registerAsyncActivity(context1);
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        ActivityStartOutsideTransaction activity1 = new ActivityStartOutsideTransaction(context1, false);
        activity1.start();
        activity1.join();

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(1, tx.getFinishedChildren().size());
        List<Tracer> tracers = tx.getTracers();
        for (Tracer current : tracers) {
            Assert.assertFalse(current.getTransactionSegmentName().contains("outsideTrans" + context1));
        }
    }

    class ActivityStartOutsideTransaction extends Thread {
        Object context;
        boolean atBeginning;

        public ActivityStartOutsideTransaction(Object context, boolean startBegining) {
            this.context = context;
            this.atBeginning = startBegining;
        }

        @Override
        public void run() {
            try {
                Transaction.clearTransaction();
                Transaction tx = Transaction.getTransaction();
                if (atBeginning) {
                    ServiceFactory.getAsyncTxService().startAsyncActivity(context);
                }
                Tracer rootTracer = createDispatcherTracer("outsideTrans" + context.toString());
                Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);
                Transaction.getTransaction().getTransactionActivity().tracerFinished(rootTracer, 0);
                if (!atBeginning) {
                    ServiceFactory.getAsyncTxService().startAsyncActivity(context);
                }

            } catch (Exception e) {
                Assert.fail("An exception should not have been thrown: " + e.getMessage());
            }
        }
    }

    // Create a Tracer for tests that require one.
    private BasicRequestRootTracer createDispatcherTracer(String methodName) {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), methodName, "()V");
        BasicRequestRootTracer brrt = new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
        tx.setDispatcher(brrt.createDispatcher());
        return brrt;
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        data = transactionData;
        stats = transactionStats;

    }

    private void waitForTransaction() {
        long start = System.currentTimeMillis();
        // Wait for data to be available
        while ((System.currentTimeMillis() - start) < 10000 && (data == null || stats == null)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

}
