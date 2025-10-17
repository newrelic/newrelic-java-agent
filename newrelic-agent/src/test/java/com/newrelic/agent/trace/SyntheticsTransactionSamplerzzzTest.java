/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.commands.CommandParser;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SyntheticsTransactionSamplerzzzTest {

    // There can really only be one test in this class so it'd better be a good one. ;-)

    static final AtomicInteger totalEnqueued = new AtomicInteger(0);
    static final AtomicInteger totalDropped = new AtomicInteger(0);
    static final AtomicInteger totalDequeued = new AtomicInteger(0);
    private static final int N_TO_ENQUEUE = 200;
    private static final int N_GENERATOR_THREADS = 10;
    private static final String APP_NAME = "SynTestApp";

    @Before
    public void setup() throws Exception {
        totalEnqueued.set(0);
        totalDropped.set(0);
        totalDequeued.set(0);
        createServiceManager(createConfigMap());
    }

    @After
    public void teardown() throws Exception {
        ServiceFactory.getServiceManager().stop();
        ServiceFactory.setServiceManager(null);
    }

    /*
     * This isn't really a test. Instead, it estimates the likelihood of hitting the window in the noticeTransaction()
     * method. The window allows more than 20 transactions to be queued if the 20th, 21st, ... transactions occur within
     * a short time (the time required to do an arithmetic test, not branch, and increment an atomic integer) of the
     * 20th transaction in the minute. The window is probably just a few nanoseconds on most hosts, but for the sake of
     * argument here we treat it as being 1,000 nanoseconds (1 microsecond, 0.000001 seconds).
     *
     * So this test generates 50,000 doubles (50,000RPM) between 0 and 60, each one representing a transaction
     * completion time within the minute. It then sorts them and checks whether the 20th, 21, ... are within one
     * millionth of a second of each other. It runs for a total of two seconds. On my Mac it does a few hundred trials
     * in these 2 seconds (where each trial is a simulated minute at 50K synthetic RPM) and usually gets no "hits"
     * (where a hit is a 21st transaction leaking through). I seem to get about 1 hit every 1000 trial "minutes".
     *
     * Note: the "20" above is actually a constant in the sampler, so we do use the constant in the code below.
     */

    private static final boolean runWindowSimulator = false;
    private static final int SimulatedRPM = 50000;
    private static final double TimeWindowInMicroseconds = 1.0;

    @Test
    public void testLikelihoodOfHittingPendingCountWindowInNoticeTransaction() {
        if (!runWindowSimulator) {
            return;
        }

        long tStart = System.currentTimeMillis();
        Random r = new Random();
        int hits = 0;
        int tries = 0;
        while (System.currentTimeMillis() < tStart + 2000) {
            // Simulate one minute of operation with 50,000 synthetic transactions
            tries++;
            double[] a = new double[SimulatedRPM];
            for (int i = 0; i < a.length; ++i) {
                // Each a[i] is a transaction completion time within the minute
                a[i] = 60.0 * r.nextDouble();
            }
            Arrays.sort(a);
            // Check if the 21 tx completed very close to the 20th, and then if so,
            // whether the 22nd completed very close to the 21st and so on.
            for (int i = SyntheticsTransactionSampler.MAX_SYNTHETIC_TRANSACTION_PER_HARVEST; i < a.length - 1; ++i) {
                if (a[i + 1] - a[i] > TimeWindowInMicroseconds / (1000.0 * 1000.0)) {
                    break;
                }
                System.out.println("a[i + 1] = " + a[i + 1] + " a[i] = " + a[i]);
                hits++;
            }
        }
        System.out.println("testLikelihood: " + hits + " hits after " + tries + " tries.");
    }

    private static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        return map;
    }

    private static void createServiceManager(Map<String, Object> configMap) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);
        serviceManager.setConfigService(configService);

        DatabaseService dbService = new DatabaseService();
        serviceManager.setDatabaseService(dbService);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setIsConnected(true);
        rpmServiceManager.setRPMService(rpmService);

        CommandParser commandParser = new CommandParser();
        serviceManager.setCommandParser(commandParser);

        AttributesService attService = new AttributesService();
        serviceManager.setAttributesService(attService);

        TransactionTraceService ttService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(ttService);
        ttService.start();
    }

    static class TxGeneratorThread extends Thread {

        private final SyntheticsTransactionSampler sampler;
        private final int nToEnqueue;
        private final Random r = new Random();

        public TxGeneratorThread(SyntheticsTransactionSampler sampler, int nToEnqueue) {
            this.sampler = sampler;
            this.nToEnqueue = nToEnqueue;
        }

        @Override
        public void run() {
            // Simulate a bunch of transactions completing and enqueuing traces
            for (int i = 0; i < nToEnqueue; ++i) {
                if (r.nextDouble() < 0.3) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                    }
                }
                TransactionData td = createTransactionData(5 + r.nextInt(1000));
                if (sampler.noticeTransaction(td)) {
                    totalEnqueued.incrementAndGet();
                } else {
                    totalDropped.incrementAndGet();
                }
            }
        }

        private TransactionData createTransactionData(long durationInMillis) {
            MockDispatcherTracer rootTracer = new MockDispatcherTracer();
            rootTracer.setDurationInMilliseconds(durationInMillis);
            rootTracer.setStartTime(System.nanoTime());
            rootTracer.setEndTime(System.nanoTime()
                    + TimeUnit.NANOSECONDS.convert(durationInMillis, TimeUnit.MILLISECONDS));

            AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

            return new TransactionDataTestBuilder(APP_NAME, agentConfig, rootTracer)
                    .setDispatcher(new MockDispatcher())
                    .setRequestUri("testTx")
                    .setFrontendMetricName("testTx")
                    .setSynJobId("jobID")
                    .setSynMonitorId("monitorID")
                    .setSynResourceId("resourceID")
                    .build();
        }
    }

    static class AcceleratedHarvestThread extends Thread {
        private final SyntheticsTransactionSampler sampler;
        private volatile boolean done = false;

        public AcceleratedHarvestThread(SyntheticsTransactionSampler sampler) {
            this.sampler = sampler;
        }

        @Override
        public void run() {
            while (!done) {
                totalDequeued.addAndGet(sampler.harvest(APP_NAME).size());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // typically we'll come through here at shutdown() time
                }
            }

            // All generator threads have completed and all their work is
            // visible across threads because of the join() calls. Do one
            // more harvest to pick up their work.
            totalDequeued.addAndGet(sampler.harvest(APP_NAME).size());
        }

        public void shutdown() {
            done = true;
            // we should do an interrupt here, but there are some complexities
            // and not doing it will only cost us at most 20 milliseconds.
        }
    }

    @Test
    public void testLocklessBoundedBuffer() throws Exception {

        // This test looks nondeterministic, but it should be fully deterministic.
        // Some threads simulate transactions completing, and each completion tries
        // to enqueue a trace; if the enqueuing succeeds, the thread increments
        // totalEnqueued. At the same time, another thread tries to dequeue all the
        // enqueued things. The total number of dequeued things should match the
        // number of enqueued things. And after all the enqueuing threads complete,
        // the very next dequeue should empty the queue; there should be no lag.

        SyntheticsTransactionSampler sampler = new SyntheticsTransactionSampler();

        Thread[] generators = new Thread[N_GENERATOR_THREADS];
        for (int i = 0; i < generators.length; ++i) {
            generators[i] = new TxGeneratorThread(sampler, N_TO_ENQUEUE);
        }

        AcceleratedHarvestThread harvester = new AcceleratedHarvestThread(sampler);
        harvester.start();

        for (int i = 0; i < generators.length; ++i) {
            generators[i].start();
        }
        for (int i = 0; i < generators.length; ++i) {
            generators[i].join();
        }

        harvester.shutdown();
        harvester.join();

        // System.out.println("Enqueued = " + totalEnqueued.get() + " dequeued = " + totalDequeued.get() + " dropped = "
        // + totalDropped.get());
        // (1) All the fake traces we generated should have been either queued for harvest or dropped
        Assert.assertEquals(N_TO_ENQUEUE * N_GENERATOR_THREADS, totalEnqueued.get() + totalDropped.get());
        // (2) All the traces that got queued should have gotten dequeued
        Assert.assertEquals(totalEnqueued.get(), totalDequeued.get());
        // (3) The count of pending TransactionDatas for harvest in the sampler should have converged on zero.
        Assert.assertEquals(0, sampler.getPendingCount());
        // (4) If we didn't enqueue any (or dequeue any), something is probably wrong with the test itself -
        // for example, the fake TransactionData objects perhaps don't appear to be from synthetic transactions.
        Assert.assertNotEquals(0, totalEnqueued.get());
        Assert.assertNotEquals(0, totalDequeued.get());
    }
}
