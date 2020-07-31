/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.deadlock;

import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.errors.TracedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class DeadlockDetectorTest {
    private Thread t1;
    private Thread t2;

    private Thread makeThread(
            final ReentrantLock firstLock,
            final CyclicBarrier otherThreadHasTheirLock,
            final ReentrantLock secondLock) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                firstLock.lock();

                try {
                    otherThreadHasTheirLock.await();
                } catch (InterruptedException | BrokenBarrierException ignored) {
                }
                try {
                    secondLock.lockInterruptibly();
                } catch (InterruptedException ignored) {
                }
            }
        });

        thread.setDaemon(true);
        return thread;
    }

    @Before
    public void before() {
        final ReentrantLock lock1 = new ReentrantLock();
        final ReentrantLock lock2 = new ReentrantLock();
        final CyclicBarrier otherThreadHasTheirLock = new CyclicBarrier(2);

        t1 = makeThread(lock1, otherThreadHasTheirLock, lock2);
        t2 = makeThread(lock2, otherThreadHasTheirLock, lock1);

        t1.start();
        t2.start();
    }

    @After
    public void after() {
        t1.interrupt();
        t2.interrupt();
    }

    @Test
    public void deadlock() throws InterruptedException {
        ErrorCollectorConfig config = mock(ErrorCollectorConfig.class);
        DeadLockDetector deadlockDetector = new DeadLockDetector(config);
        Thread.sleep(1000);
        ThreadInfo[] deadlockedThreadInfos = deadlockDetector.getDeadlockedThreadInfos();
        int count = 0;
        while (deadlockedThreadInfos.length < 2 && count < 5) {
            count++;
            Thread.sleep(1000);
            deadlockedThreadInfos = deadlockDetector.getDeadlockedThreadInfos();
        }
        assertEquals(2, deadlockedThreadInfos.length);

        assertTrue(deadlockedThreadInfos[0].getStackTrace().length > 0);
        TracedError[] tracedErrors = deadlockDetector.getTracedErrors(Arrays.asList(deadlockedThreadInfos));
        assertEquals(1, tracedErrors.length);

        Map<String, Collection<String>> stackTraces = tracedErrors[0].stackTraces();
        assertTrue(stackTraces.containsKey(deadlockedThreadInfos[0].getThreadName()));
        assertTrue(stackTraces.containsKey(deadlockedThreadInfos[1].getThreadName()));
    }
}
