/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.AgentCollectionFactory;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import org.junit.Assert;
import org.objectweb.asm.Opcodes;

import java.util.Collection;
import java.util.Map;

public class TransactionAsyncUtility {

    public abstract static class Activity extends Thread {
        public boolean wasTxEqual = false;
        public boolean isTxaNotNull = false;
    }

    /**
     * There is already a transaction running when the link occurs.
     */
    public static class StartAndThenLink extends Activity {
        private Transaction transaction;
        private TokenImpl token;
        private boolean expireBefore;
        private boolean expireAfter;

        public StartAndThenLink(TokenImpl context, boolean expireTokenBeforeEnd, boolean expireTokenAfterEnd) {
            this.token = context;
            expireBefore = expireTokenBeforeEnd;
            expireAfter = expireTokenAfterEnd;
        }

        public StartAndThenLink(Transaction context, boolean expireTokenBeforeEnd, boolean expireTokenAfterEnd) {
            this.transaction = context;
            expireBefore = expireTokenBeforeEnd;
            expireAfter = expireTokenAfterEnd;
        }

        @Override
        public void run() {
            try {
                Transaction.clearTransaction();
                TransactionActivity.clear();

                if (token == null) {
                    // Try to get and link the token as close together as possible to prevent extra timeouts
                    token = (TokenImpl) transaction.getToken();
                }
                Tracer rootTracer = createOtherTracer("root" + token.toString());
                TransactionActivity txa = Transaction.getTransaction().getTransactionActivity();
                txa.tracerStarted(rootTracer);

                token.link();
                // these are essentially asserts
                wasTxEqual = (token.getTransaction().getTransactionIfExists() == Transaction.getTransaction(false));
                isTxaNotNull = (TransactionActivity.get() == txa);
                if (expireBefore) {
                    token.expire();
                }

                rootTracer.finish(Opcodes.RETURN, 0);

                if (expireAfter) {
                    token.expire();
                }
            } catch (Exception e) {
                Assert.fail("An exception should not have been thrown: " + e.getMessage());
            }
        }
    }

    // Create a Tracer for tests that require one.
    public static OtherRootTracer createOtherTracer(String methodName) {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("MyClass", methodName, "()V");
        OtherRootTracer brrt = new OtherRootTracer(tx, sig, new Object(), new SimpleMetricNameFormat(methodName));
        return brrt;
    }

    // Create a Tracer for tests that require one.
    public static DefaultTracer createDefaultTracer(String methodName) {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("MyClass", methodName, "()V");
        DefaultTracer brrt = new DefaultTracer(tx, sig, new Object(),
                new SimpleMetricNameFormat("Custom/" + methodName));
        return brrt;
    }

    public static void createServiceManager(Map<String, Object> map) throws Exception {
        // Initialize AgentBridge with real Caffeine factory for tests
        AgentBridge.collectionFactory = new AgentCollectionFactory();

        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(map);
        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);


        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        AsyncTransactionService asyncTxService = new AsyncTransactionService();
        serviceManager.setAsyncTransactionService(asyncTxService);

        serviceManager.setStatsService(new StatsServiceImpl());

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        serviceManager.setAttributesService(new AttributesService());

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);

        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));

        serviceManager.setExpirationService(new ExpirationService());
        serviceManager.start();
    }

    public static Collection<Tracer> basicDataVerify(TransactionData data, TransactionStats stats, Activity activity,
            int count) {
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);
        Assert.assertTrue(activity.wasTxEqual);
        Assert.assertTrue(activity.isTxaNotNull);

        Assert.assertEquals(count, data.getTransactionActivities().size());
        Collection<Tracer> tracers = data.getTracers();
        Assert.assertEquals(count, tracers.size() + 1);

        for (Tracer current : tracers) {
            String segmentName = current.getTransactionSegmentName();
            Assert.assertTrue(segmentName.startsWith("RequestDispatcher/com.newrelic.agent.transaction.TransactionAsync")
                    || segmentName.startsWith("Java/java.lang.Object/rootcom.newrelic.agent.TokenImpl@"));

        }

        TransactionAsyncUtility.verifyUnscoped(stats);

        return tracers;
    }

    public static void verifyUnscoped(TransactionStats stats) {
        Map<String, StatsBase> unscoped = stats.getUnscopedStats().getStatsMap();
        Assert.assertNotNull(unscoped.get("WebTransaction"));
        Assert.assertNotNull(unscoped.get("HttpDispatcher"));
        Assert.assertNotNull(unscoped.get("WebTransaction/Uri/Unknown"));
        Assert.assertNotNull(unscoped.get("WebTransactionTotalTime"));
        Assert.assertNotNull(unscoped.get("WebTransactionTotalTime/Uri/Unknown"));
    }

    // Create a Tracer for tests that require one.
    public static BasicRequestRootTracer createDispatcherTracer(Object thisObj, String methodName) {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(thisObj.getClass().getName(), methodName, "()V");
        BasicRequestRootTracer brrt = new BasicRequestRootTracer(tx, sig, thisObj, httpRequest, httpResponse);
        tx.setDispatcher(brrt.createDispatcher());
        return brrt;
    }

    public static BasicRequestRootTracer createAndStartDispatcherTracer(Object thisObj, String methodName, MockHttpRequest httpRequest) {
        Transaction tx = Transaction.getTransaction();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(thisObj.getClass().getName(), methodName, "()V");
        BasicRequestRootTracer brrt = new BasicRequestRootTracer(tx, sig, thisObj, httpRequest, httpResponse);
        tx.getTransactionActivity().tracerStarted(brrt);
        tx.setDispatcher(brrt.createDispatcher());
        return brrt;
    }

}
