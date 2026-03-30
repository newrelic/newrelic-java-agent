/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.threads;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.util.AgentCollectionFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.api.agent.Trace;
import org.junit.Before;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThreadStateSamplerTest {

    private MockServiceManager serviceManager;

    @Before
    public void setUp() {
        // Initialize AgentBridge with real Caffeine factory for tests
        AgentBridge.collectionFactory = new AgentCollectionFactory();

        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.setStatsService(new StatsServiceImpl());
    }

    /**
     * Run to get some performance numbers related to cache cleanup
     */
    @Test
    public void testCleanupPerformanceWithDifferentCacheSizes() {
        ThreadStateSampler threadStateSampler = new ThreadStateSampler(
                ManagementFactory.getThreadMXBean(),
                ThreadNameNormalizerTest.getThreadNameNormalizer());

        System.out.println("\n=== Cleanup Performance Test ===");

        threadStateSampler.run();

        int cacheSize = threadStateSampler.getTrackedThreadCount();
        System.out.println("Initial cache size: " + cacheSize);

        // Test cleanup performance - measure getThreadInfo() calls
        long startTime = System.nanoTime();

        // This simulates what cleanupDeadThreads() does
        int deadThreadCount = 0;
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        // Simulate checking many thread IDs (including dead ones)
        for (long threadId = 1; threadId < cacheSize * 10000; threadId++) {
            if (threadMXBean.getThreadInfo(threadId) == null) {
                deadThreadCount++;
            }
        }

        long endTime = System.nanoTime();
        double totalTimeMs = (endTime - startTime) / 1_000_000.0;
        int checksPerformed = cacheSize * 10000;

        System.out.println(String.format(
            "Checked %d thread IDs in %.2f ms (%.3f µs per check). Simulated an eviction of %d threads from cache.",
            checksPerformed, totalTimeMs, (totalTimeMs * 1000.0) / checksPerformed, deadThreadCount
        ));
    }

    @Test
    public void testCpuTimeMetrics() {
        StatsService statsService = ServiceFactory.getStatsService();
        ThreadStateSampler threadStateSampler = new ThreadStateSampler(ManagementFactory.getThreadMXBean(),
                ThreadNameNormalizerTest.getThreadNameNormalizer());

        CpuCalculationThread cpuCalculation = new CpuCalculationThread(threadStateSampler);
        cpuCalculation.start();
        try {
            cpuCalculation.join();
        } catch (InterruptedException e) {
        }

        StatsEngine engine = statsService.getStatsEngineForHarvest(null);
        ResponseTimeStats systemTimeStats = engine.getResponseTimeStats("Threads/Time/CPU/CALCULATE_CPU/SystemTime");
        ResponseTimeStats userTimeStats = engine.getResponseTimeStats("Threads/Time/CPU/CALCULATE_CPU/UserTime");
        ResponseTimeStats totalTimeStats = engine.getResponseTimeStats("Threads/TotalTime/CALCULATE_CPU/CpuTime");

        // these are approximate times
        float systemTimeSeconds = systemTimeStats.getTotal();
        float userTimeSeconds = userTimeStats.getTotal();
        float totalTimeSeconds = totalTimeStats.getTotal();
        float cpuCalculationTimeSeconds = (float) ((double) cpuCalculation.getCpuTime() / 1000000000) * 10;

        assertTrue("CpuTime: " + cpuCalculationTimeSeconds + ", TotalTime: " + totalTimeSeconds,
                cpuCalculationTimeSeconds >= totalTimeSeconds);
        assertTrue("UserTime: " + userTimeSeconds + ", SystemTime: " + systemTimeSeconds,
                userTimeSeconds > systemTimeSeconds);
        // This test is prone to flickering due to high load scenarios and rounding errors, hence the modifier added to totalTimeSeconds
        assertTrue("TotalTime: " + totalTimeSeconds + ", SystemTime: " + systemTimeSeconds + ", UserTime: " + userTimeSeconds,
                totalTimeSeconds + 4.0 >= systemTimeSeconds + userTimeSeconds); // account for rounding error

        // Since we can't guarantee the exact total time, it should be
        // between 1 and 5 (since we had a busywork loop for ~5000ms)
        assertTrue(totalTimeSeconds > 1.0f);
        assertTrue(totalTimeSeconds < 6.0f);
    }

    private class CpuCalculationThread extends Thread {

        private long cpuTimeForThread;
        private ThreadStateSampler threadStateSampler;

        public CpuCalculationThread(ThreadStateSampler threadStateSampler) {
            super("CALCULATE_CPU");
            this.threadStateSampler = threadStateSampler;
        }

        @Trace(dispatcher = true)
        @Override
        public void run() {
            long startTime = ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime();
            threadStateSampler.run();
            busyWork(5000);
            threadStateSampler.run();
            long endTime = ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime();
            cpuTimeForThread = endTime - startTime;
        }

        private void busyWork(long msToSleep) {
            long startTs = System.currentTimeMillis();
            while (startTs + msToSleep > System.currentTimeMillis()) {
            }
        }

        public long getCpuTime() {
            return cpuTimeForThread;
        }
    }

}
