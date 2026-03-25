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

    @Test
    public void liveThreadsShouldNotBeRemovedDuringCleanup() throws Exception {
        ThreadStateSampler threadStateSampler = new ThreadStateSampler(
                ManagementFactory.getThreadMXBean(),
                ThreadNameNormalizerTest.getThreadNameNormalizer());

        int initialCacheSize = threadStateSampler.getTrackedThreadCount();

        // Create long-lived threads that will stay alive throughout the test
        Thread[] longLivedThreads = new Thread[5];
        for (int i = 0; i < longLivedThreads.length; i++) {
            final int threadNum = i;
            longLivedThreads[i] = new Thread(() -> {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "LongLivedThread-" + threadNum);
            longLivedThreads[i].start();
        }

        // Sample to add threads to the cache
        threadStateSampler.run();
        int cacheAfterFirstSample = threadStateSampler.getTrackedThreadCount();
        assertTrue("Cache should contain more threads after sampling",
                cacheAfterFirstSample > initialCacheSize);

        // Trigger cleanup
        for (int i = 0; i < 4; i++) {
            threadStateSampler.run();
        }

        // Verify live threads aare still around
        int cacheAfterCleanup = threadStateSampler.getTrackedThreadCount();
        assertTrue("Live threads should still be in cache after cleanup",
                cacheAfterCleanup >= cacheAfterFirstSample);

        // Nuke long-running threads manually
        for (Thread thread : longLivedThreads) {
            thread.interrupt();
        }
        for (Thread thread : longLivedThreads) {
            thread.join(1000);
        }
    }

    @Test
    public void shortLivedThreadsShouldBeEvictedFromCache() throws Exception {
        ThreadStateSampler threadStateSampler = new ThreadStateSampler(
                ManagementFactory.getThreadMXBean(),
                ThreadNameNormalizerTest.getThreadNameNormalizer());

        int initialCacheSize = threadStateSampler.getTrackedThreadCount();

        // Create and start 10 short-lived threads that will terminate quickly
        Thread[] shortLivedThreads = new Thread[10];
        for (int i = 0; i < shortLivedThreads.length; i++) {
            final int threadNum = i;
            shortLivedThreads[i] = new Thread(() -> {
                try {
                    // Just sleep for a short time and exit
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "ShortLivedThread-" + threadNum);
            shortLivedThreads[i].start();
        }

        // Sample to add threads to the cache
        threadStateSampler.run();
        int cacheAfterFirstSample = threadStateSampler.getTrackedThreadCount();
        assertTrue("Cache should contain more threads after sampling",
                cacheAfterFirstSample > initialCacheSize);

        // Wait for all short-lived threads to terminate and verify they're dead
        for (Thread thread : shortLivedThreads) {
            thread.join(5000); // Wait up to 5 seconds for each thread
            assertFalse("Thread should be terminated: " + thread.getName(), thread.isAlive());
        }

        // Trigger cleanup
        for (int i = 0; i < 4; i++) {
            threadStateSampler.run();
        }

        int cacheAfterCleanup = threadStateSampler.getTrackedThreadCount();
        assertEquals("Dead threads should be removed from cache after cleanup cycle",
                initialCacheSize, cacheAfterCleanup, cacheAfterFirstSample - initialCacheSize);
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
