/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TransactionServiceTest {

    private static final String APP_NAME = "Unit Test";

    @Before
    public void setup() throws Exception {
        createServiceManager(createConfigMap());
    }

    @After
    public void teardown() throws Exception {
        ServiceFactory.getServiceManager().stop();
        ServiceFactory.setServiceManager(null);
    }

    private Map<String, Object> createConfigMap() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("host", "staging-collector.newrelic.com");
        configMap.put("port", 80);
        configMap.put(AgentConfigImpl.APP_NAME, APP_NAME);
        Map<String, Object> errorCollectorMap = createMap();
        configMap.put("error_collector", errorCollectorMap);
        return configMap;
    }

    private static Map<String, Object> createMap() {
        return new HashMap<>();
    }

    private MockServiceManager createServiceManager(Map<String, Object> map) throws Exception {

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
        serviceManager.setConfigService(configService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        transactionService.start();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        ExtensionService extensionService = new ExtensionService(configService, ExtensionsLoadedListener.NOOP);
        serviceManager.setExtensionService(extensionService);

        TracerService tracerService = new TracerService();
        serviceManager.setTracerService(tracerService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

        EnvironmentService environmentService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(environmentService);

        AttributesService attributesService = new AttributesService();
        serviceManager.setAttributesService(attributesService);

        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));

        return serviceManager;
    }
    
    @Test
    public void testDispatcherTransactionStarted() {
        final AtomicReference<Transaction> startedTransaction = new AtomicReference<>(null);
        final AtomicReference<Transaction> finishedTransaction = new AtomicReference<>(null);
        ServiceFactory.getTransactionService().addTransactionListener(new ExtendedTransactionListener() {
            @Override
            public void dispatcherTransactionStarted(Transaction transaction) {
                startedTransaction.set(transaction);
            }

            @Override
            public void dispatcherTransactionCancelled(Transaction transaction) {

            }

            @Override
            public void dispatcherTransactionFinished(TransactionData transactionData,TransactionStats transactionStats) {
                finishedTransaction.set(transactionData.getTransaction());
            }
        });

        Tracer tracer = makeTransaction();

        assertNotNull(tracer);
        assertNotNull(startedTransaction.get());
        assertNull(finishedTransaction.get());
        assertEquals(tracer.getTransactionActivity().getTransaction(), startedTransaction.get());
    }

    @Test
    public void testDispatcherTransactionFinished() {
        final AtomicReference<Transaction> finishedTransaction = new AtomicReference<>(null);
        final AtomicReference<Transaction> finishedTransactionExtended = new AtomicReference<>(null);
        ServiceFactory.getTransactionService().addTransactionListener(new ExtendedTransactionListener() {
            @Override
            public void dispatcherTransactionStarted(Transaction transaction) {

            }

            @Override
            public void dispatcherTransactionCancelled(Transaction transaction) {

            }

            @Override
            public void dispatcherTransactionFinished(TransactionData transactionData,TransactionStats transactionStats) {
                finishedTransactionExtended.set(transactionData.getTransaction());
            }
        });
        
        ServiceFactory.getTransactionService().addTransactionListener(new TransactionListener() {
            @Override
            public void dispatcherTransactionFinished(TransactionData transactionData,TransactionStats transactionStats) {
                finishedTransaction.set(transactionData.getTransaction());
            }
        });

        Tracer tracer = makeTransaction();
        tracer.finish(0, null);
        tracer.getParentTracer().finish(0, null);

        assertNotNull(tracer);
        assertNotNull(finishedTransaction.get());
        assertNotNull(finishedTransactionExtended.get());
        assertEquals(tracer.getTransactionActivity().getTransaction(), finishedTransaction.get());
        assertEquals(tracer.getTransactionActivity().getTransaction(), finishedTransactionExtended.get());
    }

    @Test
    public void testRandomnessPriority() throws InterruptedException {
        final List<TransactionEvent> events = new ArrayList<>();
        ServiceFactory.getServiceManager().getTransactionService()
                .addTransactionListener(new TransactionListener() {
                    @Override
                    public void dispatcherTransactionFinished(TransactionData transactionData,
                            TransactionStats transactionStats) {
                        events.add(ServiceFactory.getTransactionEventsService().createEvent(transactionData,
                                transactionStats, transactionData.getBlameOrRootMetricName()));
                    }
                });

        for (int i = 0; i < 100; i++) {
            Transaction.clearTransaction();
            Tracer rootTracer = makeTransaction();
            Transaction tx = rootTracer.getTransactionActivity().getTransaction();

            tx.getTransactionActivity().tracerStarted(rootTracer);
            rootTracer.finish(Opcodes.RETURN, 0);

            Assert.assertTrue(tx.isFinished());
        }

        Thread.sleep(1000);
        Assert.assertFalse(events.isEmpty());
        Assert.assertTrue(events.size() == 100);
        float first = events.get(0).getPriority();
        float second = events.get(1).getPriority();
        float third = events.get(2).getPriority();
        float forth = events.get(3).getPriority();
        Assert.assertFalse(first == second && first == third && first == forth);
    }

    @Test
    public void testServiceDisabled() throws Exception {
        TransactionService service = ServiceFactory.getServiceManager().getTransactionService();
        service.stop();

        service.transactionStarted(Mockito.mock(Transaction.class));
        service.transactionStarted(Mockito.mock(Transaction.class));
        service.transactionStarted(Mockito.mock(Transaction.class));

        assertEquals(0, service.getTransactionsInProgress());
    }

    private static Tracer makeTransaction() {
        Transaction tx = Transaction.getTransaction();
        TransactionActivity txa = TransactionActivity.get();
        assertNotNull(txa);
        Tracer root = new OtherRootTracer(tx, new ClassMethodSignature("com.newrelic.agent.SegmentTest",
                "makeTransaction", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        txa.tracerStarted(root);
        tx.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Category", "TxName");
        return root;
    }
}
