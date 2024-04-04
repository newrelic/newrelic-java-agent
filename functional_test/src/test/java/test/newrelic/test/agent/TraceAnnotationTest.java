/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.GET;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class TraceAnnotationTest implements TransactionListener {

    private volatile TransactionStats tranStats;

    @Before
    public void setup() {
        ServiceFactory.getStatsService().getStatsEngineForHarvest(null).clear();
        ServiceFactory.getTransactionService().addTransactionListener(this);
        tranStats = null;
        Transaction.clearTransaction();
    }

    @After
    public void tearDown() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @Test
    public void testInstrumentedMethodMarker() throws SecurityException, NoSuchMethodException {
        Method method = Simple.class.getDeclaredMethod("foo");
        InstrumentedMethod annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(InstrumentationType.TraceAnnotation, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("TraceAnnotationTest.java", annotation.instrumentationNames()[0]);
    }

    @Test
    public void testUnInstrumentedMethodNoMarker() throws SecurityException, NoSuchMethodException {
        Method method = Simple.class.getMethod("uninstrumentedMethod");
        Assert.assertFalse(method.isAnnotationPresent(InstrumentedMethod.class));
    }

    @Test
    public void testSimpleAnnotation() throws Exception {
        callFoo();

        // Assert.assertTrue(Agent.instance().getApplicationNames().contains("Unknown"));
        Set<String> metrics = AgentHelper.getMetrics();

        Assert.assertTrue(metrics.toString(), metrics.contains(MessageFormat.format("Custom/{0}/foo",
                Simple.class.getName())));
        Assert.assertTrue(metrics.toString(), metrics.contains("Custom/Testing"));
    }

    @Trace(dispatcher = true)
    private void callFoo() throws InterruptedException {
        new Simple().foo();
        new Simple().foo(200d);
    }

    @Test
    public void testCustomTracerFactory() {
        ServiceFactory.getTracerService().registerTracerFactory("DudeFactory", new MyTracerFactory());

        callCustomFactory();
    }

    @Trace(dispatcher = true)
    private void callCustomFactory() {
        Assert.assertTrue(new Simple().customTracerFactory().containsKey(MyTracerFactory.DUDE));
    }

    @Test
    public void testRollup() throws Exception {
        Transaction transaction = new Callable<Transaction>() {

            @Trace(dispatcher = true, rollupMetricName = "Rollup1")
            @Override
            public Transaction call() throws Exception {
                return Transaction.getTransaction();
            }

        }.call();

        Assert.assertNotNull(tranStats);
        ResponseTimeStats stats = tranStats.getUnscopedStats().getOrCreateResponseTimeStats("Rollup1");

        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
    }

    @Test
    public void testCustomAnnotation() throws Exception {
        callCustom();

        Set<String> metrics = AgentHelper.getMetrics();

        String expectedName = MessageFormat.format("Custom/{0}/dude", Custom.class.getName());
        Assert.assertTrue(expectedName + " not in " + metrics.toString(), metrics.contains(expectedName));
    }

    @Trace(dispatcher = true)
    private void callCustom() {
        new Custom().dude();
    }

    @Test
    public void testIgnoreTxAnnotation() throws Exception {
        Runnable test = new Runnable() {

            @Override
            @Trace(dispatcher = true)
            public void run() {
                Transaction t = Transaction.getTransaction();
                Assert.assertFalse(t.isIgnore());
                call();
                Assert.assertTrue(t.isIgnore());
            }

            @CustomTrace
            public void call() {
                ignore();
            }

            @NewRelicIgnoreTransaction
            public void ignore() {
            }

        };

        test.run();
    }

    @Trace(dispatcher = true, metricName = "Dude")
    @Test
    public void testDispatcherWithMetricName() throws Exception {
        restService();
        // Assert.assertNotEquals("OtherTransaction/Custom/Dude",
        // Transaction.getTransaction().getPriorityTransactionName().getName());
        // Transaction.getTransaction().freezeTransactionName();

        Assert.assertEquals("OtherTransaction/Custom/Dude",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @GET
    private void restService() {

    }

    @Test
    public void testDispatcherChangeTransactionName() throws Exception {
        Transaction tx = Transaction.getTransaction();
        Assert.assertEquals(PriorityTransactionName.NONE.getName(), tx.getPriorityTransactionName().getName());

        new Runnable() {
            @Override
            @Trace(dispatcher = true)
            public void run() {
                Transaction.getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "Test",
                        "MyTest");
                // Freezing tx name here keeps the name as expected.
                // Transaction.getTransaction().freezeTransactionName();
                new Runnable() {
                    @Override
                    @Trace(dispatcher = true, metricName = "Dude")
                    public void run() {
                    }
                }.run();
            }
        }.run();

        // Transaction.getTransaction().freezeTransactionName();

        // Should be
        // Assert.assertEquals("OtherTransaction/Test/MyTest", tx.getPriorityTransactionName().getName());
        // Instead is
        Assert.assertEquals("OtherTransaction/Custom/Dude", tx.getPriorityTransactionName().getName());
    }

    @Test
    public void testDispatcher() throws Exception {
        new Simple().dispatch();

        List<MetricName> metrics = AgentHelper.getDefaultStatsEngine().getMetricNames();

        MetricName dispatcherMetricName = MetricName.create(MessageFormat.format("{0}/{1}/dispatch",
                MetricNames.OTHER_TRANSACTION_CUSTOM, Simple.class.getName()));
        Assert.assertTrue(metrics.contains(dispatcherMetricName));
        MetricName child = MetricName.create("Custom/Testing", dispatcherMetricName.getName());
        Assert.assertTrue(metrics.contains(child));
        Assert.assertTrue(metrics.contains(MetricName.create(MetricNames.OTHER_TRANSACTION_ALL)));
    }

    @Test
    public void testPrimitiveArgs() throws Exception {
        callPrimitive();

        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyMetrics(metrics, MessageFormat.format("Custom/{0}/primitive", Simple.class.getName()));
    }

    @Trace(dispatcher = true)
    private void callPrimitive() {
        new Simple().primitive(0);
    }

    @Test
    public void testArrayArgs() throws Exception {
        callArrayArg();

        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyMetrics(metrics, MessageFormat.format("Custom/{0}/arrayArg", Simple.class.getName()));
    }

    @Trace(dispatcher = true)
    private void callArrayArg() throws InterruptedException {
        new Simple().arrayArg(new String[] { "dude" });
    }

    @Test
    public void testDoubleArrayArgs() throws Exception {
        callDoubleArray();

        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyMetrics(metrics, MessageFormat.format("Custom/{0}/doubleArrayArg", Simple.class.getName()));
    }

    @Test
    public void testOTelWithSpan() {
        withSpan();

        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyMetrics(metrics, MessageFormat.format("Custom/{0}/withSpan", Simple.class.getName()));
    }

    @WithSpan
    private void withSpan() {

    }

    @Trace(dispatcher = true)
    private void callDoubleArray() {
        new Simple().doubleArrayArg(new String[0][0]);
    }

    /*
     * @Test public void testDoubleArrayArgs() throws Exception { new Simple().doubleArrayArg(new String[0][0]);
     * 
     * Set<String> metrics = AgentHelper.getMetrics(); AgentHelper.verifyMetrics(metrics,
     * MessageFormat.format("Custom/{0}/doubleArrayArg", Simple.class.getName())); }
     */

    @Test
    public void testIntArrayArgs() throws Exception {
        callIntArray();

        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyMetrics(metrics, MessageFormat.format("Custom/{0}/intArray", Simple.class.getName()));
    }

    @Trace(dispatcher = true)
    private void callIntArray() {
        new Simple().intArray(new int[] { 6 });
    }

    @Test
    public void testCharArrayArgs() throws Exception {
        callCharArray();

        Set<String> metrics = AgentHelper.getMetrics();
        AgentHelper.verifyMetrics(metrics, MessageFormat.format("Custom/{0}/charArray", Simple.class.getName()));
    }

    @Trace(dispatcher = true)
    private void callCharArray() {
        new Simple().charArray(new char[] { 6 });
    }

    private class Simple {
        @Trace
        private void foo() throws InterruptedException {
            Thread.sleep(200);
            ClassMethodSignature classMethodSignature = Transaction.getTransaction().getTransactionActivity().getLastTracer().getClassMethodSignature();
            Assert.assertEquals(Simple.class.getName(), classMethodSignature.getClassName());
        }

        @Trace
        private void primitive(int i) {
        }

        @Trace
        public void arrayArg(String[] strings) throws InterruptedException {
            Thread.sleep(200);
        }

        @Trace
        public void doubleArrayArg(String[][] strings) {
        }

        @Trace
        public void intArray(int[] i) {
        }

        @Trace
        public void charArray(char[] i) {
        }

        @Trace(metricName = "Custom/Testing")
        public void foo(Number n) throws InterruptedException {
            Thread.sleep(n.longValue());
        }

        @Trace(dispatcher = true)
        public void dispatch() throws InterruptedException {
            foo(20d);
        }

        @Trace(tracerFactoryName = "DudeFactory")
        public Map<String, Object> customTracerFactory() {
            Transaction transaction = Transaction.getTransaction();
            return new HashMap<>(transaction.getAgentAttributes());
        }

        public void uninstrumentedMethod() {

        }
    }

    private static class MyTracerFactory extends AbstractTracerFactory {

        static final String DUDE = "dude";

        @Override
        public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
            transaction.getAgentAttributes().put(DUDE, Boolean.TRUE);
            return new DefaultTracer(transaction, sig, object);
        }

    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomTrace {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NewRelicIgnoreTransaction {
    }

    public class Custom {
        @CustomTrace
        public void dude() {
        }
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        tranStats = transactionStats;
    }

    /* Case 1: @Trace(async = true), single threaded without a started Transaction */
    @Test
    public void testAsyncAnnotationOutsideOfTxnSingleThreaded() {
        Transaction.clearTransaction();
        asyncAnnotationOutsideOfTxnSingleThreaded();

        // AgentBridge getTransaction call should return null if no Transaction exists in ThreadLocal
        Assert.assertNull(AgentBridge.getAgent().getTransaction(false));
    }

    @Trace(async = true)
    private void asyncAnnotationOutsideOfTxnSingleThreaded() {
        // Outside of a txn, getTransaction should not create a Transaction object, but rather return a NoOpTransaction
        Assert.assertEquals("NewRelic.getAgent().getTransaction() should return a NoOpTransaction",
                com.newrelic.agent.bridge.NoOpTransaction.class, NewRelic.getAgent().getTransaction().getClass());

        // AgentBridge getTransaction call should return null if no Transaction exists in ThreadLocal
        Assert.assertNull(AgentBridge.getAgent().getTransaction(false));
    }

    /* Case 2: @Trace(async = true), single threaded with a started Transaction */
    @Test
    public void testCallAsyncAnnotationInsideOfTxnSingleThreaded() {
        Transaction.clearTransaction();
        callAsyncAnnotationInsideOfTxnSingleThreaded();

        Set<String> metrics = AgentHelper.getMetrics(true);
        // Assert that transaction metrics exist
        String txnMetric1 = "Java/test.newrelic.test.agent.TraceAnnotationTest/callAsyncAnnotationInsideOfTxnSingleThreaded";
        Assert.assertTrue("The following metric should exist: " + txnMetric1,
                metrics.contains(txnMetric1));

        String txnMetric2 = "Custom/test.newrelic.test.agent.TraceAnnotationTest/asyncAnnotationInsideOfTxnSingleThreaded";
        Assert.assertTrue("The following metric should exist: " + txnMetric2,
                metrics.contains(txnMetric2));

        // AgentBridge getTransaction call should return null if no Transaction exists in ThreadLocal
        Assert.assertNull(AgentBridge.getAgent().getTransaction(false));
    }

    @Trace(dispatcher = true)
    private void callAsyncAnnotationInsideOfTxnSingleThreaded() {
        // AgentBridge getTransaction call should return the TransactionApiImpl that exists in ThreadLocal
        Assert.assertNotNull(AgentBridge.getAgent().getTransaction(false));

        asyncAnnotationInsideOfTxnSingleThreaded();
    }

    @Trace(async = true)
    private void asyncAnnotationInsideOfTxnSingleThreaded() {
        // Inside of a txn, getTransaction should return TransactionApiImpl
        Assert.assertEquals("NewRelic.getAgent().getTransaction() should return TransactionApiImpl",
                com.newrelic.agent.TransactionApiImpl.class, NewRelic.getAgent().getTransaction().getClass());

        // AgentBridge getTransaction call should return the TransactionApiImpl that exists in ThreadLocal
        Assert.assertNotNull(AgentBridge.getAgent().getTransaction(false));
    }

    /* Case 3: @Trace(async = true), multithreaded without a started Transaction */
    @Test
    public void testCallAsyncAnnotatedThreadOutsideOfTxnMultiThreaded() throws InterruptedException {
        Transaction.clearTransaction();
        callAsyncAnnotatedThreadOutsideOfTxnMultiThreaded();

        // AgentBridge getTransaction call should return null if no Transaction exists in ThreadLocal
        Assert.assertNull(AgentBridge.getAgent().getTransaction(false));
    }

    // Don't start a transaction with @Trace(dispatcher = true)
    private void callAsyncAnnotatedThreadOutsideOfTxnMultiThreaded() throws InterruptedException {
        Token token = NewRelic.getAgent().getTransaction().getToken();
        CountDownLatch latch = new CountDownLatch(1);

        // Outside of a txn, getToken should return NoOpToken
        Assert.assertEquals("NewRelic.getAgent().getTransaction().getToken() should return NoOpToken",
                com.newrelic.agent.bridge.NoOpToken.class, token.getClass());

        // AgentBridge getTransaction call should return null if no Transaction exists in ThreadLocal
        Assert.assertNull(AgentBridge.getAgent().getTransaction(false));

        AsyncAnnotatedThreadOutsideOfTxn asyncAnnotatedThreadOutsideOfTxn = new AsyncAnnotatedThreadOutsideOfTxn(latch);
        asyncAnnotatedThreadOutsideOfTxn.start();

        latch.await();
    }

    private class AsyncAnnotatedThreadOutsideOfTxn extends Thread {
        private final CountDownLatch latch;

        AsyncAnnotatedThreadOutsideOfTxn(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        @Trace(async = true)
        public void run() {
            // Outside of a txn, getTransaction should not create a Transaction object, but rather return a NoOpTransaction
            Assert.assertEquals("NewRelic.getAgent().getTransaction() should return a NoOpTransaction",
                    com.newrelic.agent.bridge.NoOpTransaction.class, NewRelic.getAgent().getTransaction().getClass());

            // AgentBridge getTransaction call should return null if no Transaction exists in ThreadLocal
            Assert.assertNull(AgentBridge.getAgent().getTransaction(false));

            latch.countDown();
        }
    }

    /* Case 4: @Trace(async = true), multithreaded with a started Transaction */
    @Test
    public void testCallAsyncAnnotatedThreadInsideOfTxnMultiThreaded() throws InterruptedException {
        Transaction.clearTransaction();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean metricsAssertionsPassed = new AtomicBoolean(false);

        // AgentBridge getTransaction call should return null if no Transaction exists in ThreadLocal
        Assert.assertNull(AgentBridge.getAgent().getTransaction(false));

        // Transaction listener which will only countdown the latch when a specific txn finishes
        ServiceFactory.getTransactionService().addTransactionListener(new TransactionListener() {
            @Override
            public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
                PriorityTransactionName ptn = transactionData.getTransaction().getPriorityTransactionName();
                String txnMetric1 = "Java/test.newrelic.test.agent.TraceAnnotationTest$AsyncAnnotatedThreadInsideOfTxn/run";
                String txnMetric2 = "Java/test.newrelic.test.agent.TraceAnnotationTest/callAsyncAnnotatedThreadInsideOfTxnMultiThreaded";

                Map<String, StatsBase> scopedStatsMap = transactionStats.getScopedStats().getStatsMap();

                if (ptn.getPartialName().equals("/MyCategory/TracedAsyncTxn")) {
                    try {
                        Assert.assertTrue("The following metric should exist: " + txnMetric1,
                                scopedStatsMap.containsKey(txnMetric1));
                        Assert.assertTrue("The following metric should exist: " + txnMetric2,
                                scopedStatsMap.containsKey(txnMetric2));
                        metricsAssertionsPassed.set(true);
                    } catch (Throwable t) {
                        metricsAssertionsPassed.set(false);
                        t.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }
        });

        callAsyncAnnotatedThreadInsideOfTxnMultiThreaded();
        latch.await();
        Assert.assertTrue("Metric assertions didn't pass", metricsAssertionsPassed.get());
    }

    @Trace(dispatcher = true)
    private void callAsyncAnnotatedThreadInsideOfTxnMultiThreaded() {
        Token token = NewRelic.getAgent().getTransaction().getToken();

        NewRelic.getAgent().getTransaction().setTransactionName(com.newrelic.api.agent.TransactionNamePriority.CUSTOM_HIGH,
                true, "MyCategory", "TracedAsyncTxn");

        // Inside of a txn, getToken should return TokenImpl
        Assert.assertEquals("NewRelic.getAgent().getTransaction().getToken() should return TokenImpl",
                com.newrelic.agent.TokenImpl.class, token.getClass());

        // Inside of a txn, getTransaction should return TransactionApiImpl
        Assert.assertEquals("NewRelic.getAgent().getTransaction() should return TransactionApiImpl",
                com.newrelic.agent.TransactionApiImpl.class, NewRelic.getAgent().getTransaction().getClass());

        // AgentBridge getTransaction call should return the TransactionApiImpl that exists in ThreadLocal
        Assert.assertNotNull(AgentBridge.getAgent().getTransaction(false));

        AsyncAnnotatedThreadInsideOfTxn asyncAnnotatedThreadInsideOfTxn = new AsyncAnnotatedThreadInsideOfTxn(token);
        asyncAnnotatedThreadInsideOfTxn.start();
    }

    private class AsyncAnnotatedThreadInsideOfTxn extends Thread {
        private final Token token;

        AsyncAnnotatedThreadInsideOfTxn(Token token) {
            this.token = token;
        }

        @Override
        @Trace(async = true)
        public void run() {
            token.linkAndExpire();

            // Inside of a txn, getTransaction should return TransactionApiImpl
            Assert.assertEquals("NewRelic.getAgent().getTransaction() should return TransactionApiImpl",
                    com.newrelic.agent.TransactionApiImpl.class, NewRelic.getAgent().getTransaction().getClass());

            // AgentBridge getTransaction call should return the TransactionApiImpl that exists in ThreadLocal
            Assert.assertNotNull(AgentBridge.getAgent().getTransaction(false));
        }
    }
}
