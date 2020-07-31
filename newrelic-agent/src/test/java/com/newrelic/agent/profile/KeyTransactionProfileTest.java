/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.newrelic.agent.Duration;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KeyTransactionProfileTest {

    private static MockServiceManager serviceManager;

    private Profile profile;
    private KeyTransactionProfile keyTransactionProfile;

    @BeforeClass
    public static void beforeClass() throws Exception {
        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        Map<String, Object> map = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(map);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);
    }

    @Before
    public void setUp() {
        this.profile = mock(Profile.class);

        ProfilerParameters defaultProfilerParameters = new ProfilerParameters(System.currentTimeMillis(), 100, 5000,
                false, false, false, "keyTransaction", "App");
        when(profile.getProfilerParameters()).thenReturn(defaultProfilerParameters);

        this.keyTransactionProfile = Mockito.spy(new KeyTransactionProfile(profile));
    }

    @Test
    public void testKeyTransaction() {
        doReturn(new NoOpSet(Sets.newHashSet(1L))).when(keyTransactionProfile).getActiveThreadIds();

        // Create non-key transactions (followed by a key transaction on the same thread)
        keyTransactionProfile.addStackTrace(1, true, ThreadType.BasicThreadType.OTHER, generateStackTraceElements(3, "fileName"));
        long keyTransactionStartTime = System.nanoTime();
        StackTraceElement[] keyStackTrace = generateStackTraceElements(3, "fileName");
        keyTransactionProfile.addStackTrace(1, true, ThreadType.BasicThreadType.OTHER, keyStackTrace);
        long keyTransactionEndTime = System.nanoTime();

        // This should match the key transaction and leave the non-key transaction in the queue
        keyTransactionProfile.dispatcherTransactionFinished(generateTransactionData(
                ImmutableMultimap.of(1L, new Duration(keyTransactionStartTime, keyTransactionEndTime)),
                keyTransactionStartTime, keyTransactionEndTime, "keyTransaction", "App"), new TransactionStats());

        // This would get called by the sampler thread
        keyTransactionProfile.beforeSampling();

        verify(profile, times(1)).addStackTrace(
                eq(1L),
                eq(true),
                eq(ThreadType.BasicThreadType.OTHER),
                eq(keyStackTrace[0]),
                eq(keyStackTrace[1]),
                eq(keyStackTrace[2]));
    }

    @Test
    public void testUnusedThreadCleanup() {
        doReturn(new NoOpSet(Sets.newHashSet(1L))).when(keyTransactionProfile).getActiveThreadIds();

        // Create key transaction on thread 1 and the rest will be non-key transactions
        long keyTransactionStartTime = System.nanoTime();
        StackTraceElement[] keyStackTrace = generateStackTraceElements(3, "fileName");
        keyTransactionProfile.addStackTrace(1, true, ThreadType.BasicThreadType.OTHER, keyStackTrace);
        long keyTransactionEndTime = System.nanoTime();

        keyTransactionProfile.addStackTrace(2, true, ThreadType.BasicThreadType.OTHER, generateStackTraceElements(3, "fileName2"));
        keyTransactionProfile.addStackTrace(3, true, ThreadType.BasicThreadType.OTHER, generateStackTraceElements(3, "fileName3"));
        keyTransactionProfile.addStackTrace(4, true, ThreadType.BasicThreadType.OTHER, generateStackTraceElements(3, "fileName4"));
        keyTransactionProfile.addStackTrace(5, true, ThreadType.BasicThreadType.OTHER, generateStackTraceElements(3, "fileName5"));

        // This should match the key transaction and leave the non-key transactions in the queue
        keyTransactionProfile.dispatcherTransactionFinished(generateTransactionData(
                ImmutableMultimap.of(1L, new Duration(keyTransactionStartTime, keyTransactionEndTime)),
                keyTransactionStartTime, keyTransactionEndTime, "keyTransaction", "App"), new TransactionStats());

        // This would get called by the sampler thread (and should clear out the non-key transaction queues)
        keyTransactionProfile.beforeSampling();

        verify(profile, times(1)).addStackTrace(eq(1L), eq(true), eq(ThreadType.BasicThreadType.OTHER),
                eq(keyStackTrace[0]), eq(keyStackTrace[1]), eq(keyStackTrace[2]));
        assertEquals(1, keyTransactionProfile.getPendingThreadQueueSizes().size());
        assertTrue(keyTransactionProfile.getPendingThreadQueueSizes().containsKey(1L));
        assertEquals(0, (long) keyTransactionProfile.getPendingThreadQueueSizes().get(1L));
    }

    @Test
    public void testKeyTransactionsAcrossMultipleThreads() {
        doReturn(new NoOpSet(Sets.newHashSet(1L, 2L))).when(keyTransactionProfile).getActiveThreadIds();

        // Thread 1: |--- Key Tx (TxA1) ---|---  Non-Key Tx   ---|--- Key Tx (TxA3) ---|
        // Thread 2:                       |--- Key Tx (TxA2) ---|

        long key1StartTime = System.nanoTime();
        StackTraceElement[] keyStackTrace1 = generateStackTraceElements(3, "fileName1");
        keyTransactionProfile.addStackTrace(1, true, ThreadType.BasicThreadType.OTHER, keyStackTrace1);
        long key1EndTime = System.nanoTime();

        long nonKeyStartTime = System.nanoTime();
        StackTraceElement[] nonKeyStackTrace = generateStackTraceElements(3, "fileNameNonKey");
        keyTransactionProfile.addStackTrace(1, true, ThreadType.BasicThreadType.OTHER, nonKeyStackTrace);
        long nonKeyEndTime = System.nanoTime();

        keyTransactionProfile.dispatcherTransactionFinished(generateTransactionData(
                ImmutableMultimap.of(
                        1L, new Duration(nonKeyStartTime, nonKeyEndTime)
                ), nonKeyStartTime, nonKeyEndTime, "nonKeyTransaction", "App"), new TransactionStats());

        long key2StartTime = System.nanoTime();
        StackTraceElement[] keyStackTrace2 = generateStackTraceElements(3, "fileName2");
        keyTransactionProfile.addStackTrace(2, true, ThreadType.BasicThreadType.OTHER, keyStackTrace2);
        long key2EndTime = System.nanoTime();

        long key3StartTime = System.nanoTime();
        StackTraceElement[] keyStackTrace3 = generateStackTraceElements(3, "fileName3");
        keyTransactionProfile.addStackTrace(1, true, ThreadType.BasicThreadType.OTHER, keyStackTrace3);
        long key3EndTime = System.nanoTime();

        keyTransactionProfile.dispatcherTransactionFinished(generateTransactionData(
                ImmutableMultimap.of(1L, new Duration(key1StartTime, key1EndTime), 2L,
                        new Duration(key2StartTime, key2EndTime), 1L, new Duration(key3StartTime, key3EndTime)),
                key1StartTime, key3EndTime, "keyTransaction", "App"), new TransactionStats());

        // This would get called by the sampler thread
        keyTransactionProfile.beforeSampling();

        verify(profile, times(1)).addStackTrace(eq(1L), eq(true), eq(ThreadType.BasicThreadType.OTHER),
                eq(keyStackTrace1[0]), eq(keyStackTrace1[1]), eq(keyStackTrace1[2]));
        verify(profile, times(1)).addStackTrace(eq(2L), eq(true), eq(ThreadType.BasicThreadType.OTHER),
                eq(keyStackTrace2[0]), eq(keyStackTrace2[1]), eq(keyStackTrace2[2]));
        verify(profile, times(1)).addStackTrace(eq(1L), eq(true), eq(ThreadType.BasicThreadType.OTHER),
                eq(keyStackTrace3[0]), eq(keyStackTrace3[1]), eq(keyStackTrace3[2]));
        verify(profile, times(0)).addStackTrace(eq(1L), eq(true), eq(ThreadType.BasicThreadType.OTHER),
                eq(nonKeyStackTrace[0]), eq(nonKeyStackTrace[1]), eq(nonKeyStackTrace[2]));

        assertEquals(2, keyTransactionProfile.getPendingThreadQueueSizes().size());
        assertTrue(keyTransactionProfile.getPendingThreadQueueSizes().containsKey(1L));
        assertTrue(keyTransactionProfile.getPendingThreadQueueSizes().containsKey(2L));
        assertEquals(0, (long) keyTransactionProfile.getPendingThreadQueueSizes().get(1L));
        assertEquals(0, (long) keyTransactionProfile.getPendingThreadQueueSizes().get(2L));
    }

    public StackTraceElement[] generateStackTraceElements(int depth, String fileName) {
        StackTraceElement[] stackTraceElements = new StackTraceElement[depth];
        for (int i = 0; i < depth; i++) {
            stackTraceElements[i] = new StackTraceElement("declaringClass", "methodName", fileName, i);
        }
        return stackTraceElements;
    }

    private TransactionData generateTransactionData(Multimap<Long, Duration> threadIdToDuration, long startTime,
            long endTime, String blameMetricName, String appName) {
        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setWebTransaction(true);
        MockDispatcherTracer rootTracer = new MockDispatcherTracer();
        rootTracer.setDurationInMilliseconds(endTime - startTime);
        rootTracer.setStartTime(startTime);
        rootTracer.setEndTime(endTime);

        return new TransactionDataTestBuilder(
                appName,
                ServiceFactory.getConfigService().getAgentConfig(appName),
                rootTracer
        )
                .setDispatcher(rootTracer)
                .setFrontendMetricName(blameMetricName)
                .setThreadIdToDuration(threadIdToDuration)
                .build();
    }

    /**
     * A Set implementation that does not allow modification of its contents and won't throw any exceptions. We use this
     * so we can inject our own values for "Thread IDs" into the KeyTransactionProfiler and not allow it to change
     */
    private static class NoOpSet extends HashSet<Long> {

        public NoOpSet(Collection<? extends Long> c) {
            for (Long value : c) {
                super.add(value);
            }
        }

        @Override
        public boolean add(Long aLong) {
            return true;
        }

        @Override
        public boolean remove(Object o) {
            return true;
        }

        @Override
        public void clear() {
            // no-op
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Long> c) {
            return true;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return true;
        }

    }
}
