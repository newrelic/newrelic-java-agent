/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import org.junit.Test;

public class TokenFailureTest {

    @Test
    public void testCreateIndefinitely() throws Exception {
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        final AtomicBoolean interrupt = new AtomicBoolean(false);

        long startTime = System.currentTimeMillis();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getTokenIndefinitely(executorService, interrupt);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
        thread.start();

        while (thread.isAlive()) {
            if (System.currentTimeMillis() - startTime >= TimeUnit.SECONDS.toMillis(60)) {
                interrupt.set(true);
            }
            Thread.sleep(1000);
        }

        if (interrupt.get()) {
            fail("Thread did not finish normally");
        }
    }

    @Trace(dispatcher = true)
    private void getTokenIndefinitely(ExecutorService executorService, AtomicBoolean interrupt) throws Exception {
        final AtomicReference<Token> token = new AtomicReference<Token>(AgentBridge.getAgent().getTransaction(false).getToken());
        while (token.get() != null && token.get().isActive() && !interrupt.get()) {
            Future<?> result = executorService.submit(new Runnable() {
                @Override
                @Trace(async = true)
                public void run() {
                    token.get().linkAndExpire();
                    token.set(AgentBridge.getAgent().getTransaction(false).getToken());
                }
            });
            result.get(60, TimeUnit.SECONDS);
        }
        token.get().linkAndExpire();
    }

    private volatile Token tokenA;
    private volatile Token tokenB;
    private volatile Thread thread1;
    private volatile Thread thread2;

    @Test
    public void testTokenLinkDeadlock() throws Exception {
        ExecutorService deadlockDetectorExecutor = Executors.newSingleThreadExecutor();

        try {
            final AtomicInteger tokenALinkCount = new AtomicInteger(0);
            final AtomicInteger tokenBLinkCount = new AtomicInteger(0);

            // Create two threads that each start a transaction and link a token from the other thread.
            // Do this 1000 times on each thread to ensure we don't have a deadlock scenario.
            Runnable txn1 = new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        linkTokenA(tokenALinkCount);
                    }
                }
            };

            Runnable txn2 = new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        linkTokenB(tokenBLinkCount);
                    }
                }
            };

            thread1 = new Thread(txn1);
            thread2 = new Thread(txn2);

            Future<Boolean> deadlockDetectorFuture = deadlockDetectorExecutor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
                    long[] threadIds = threadBean.findMonitorDeadlockedThreads();
                    while (threadIds == null || threadIds.length == 0) {
                        threadIds = threadBean.findMonitorDeadlockedThreads();
                        Thread.sleep(100);
                        if (!thread1.isAlive() || !thread2.isAlive()) {
                            // If either thread is no longer running then we didn't hit a deadlock scenario
                            return Boolean.FALSE;
                        }
                    }
                    return Boolean.TRUE; // Deadlock detected
                }
            });

            thread1.start();
            thread2.start();
            Boolean result = deadlockDetectorFuture.get();
            assertNotNull(result);
            assertFalse(result);
            thread1.join();
            thread2.join();
        } finally {
            deadlockDetectorExecutor.shutdownNow();
        }
    }

    @Trace(dispatcher = true)
    private void linkTokenA(AtomicInteger tokenALinkCount) {
        tokenB = NewRelic.getAgent().getTransaction().getToken();
        while (tokenA == null) {
            // wait for tokenA to get set from other thread or for other thread to complete
            if (!thread2.isAlive()) {
                printTokenLinkStatus("Token A", tokenALinkCount);
                return;
            }
        }
        tokenA.linkAndExpire();
        printTokenLinkStatus("Token A", tokenALinkCount);
        tokenA = null;
    }

    @Trace(dispatcher = true)
    private void linkTokenB(AtomicInteger tokenBLinkCount) {
        tokenA = NewRelic.getAgent().getTransaction().getToken();
        while (tokenB == null) {
            // wait for tokenB to get set from other thread or for other thread to complete
            if (!thread1.isAlive()) {
                printTokenLinkStatus("Token B", tokenBLinkCount);
                return;
            }
        }
        tokenB.linkAndExpire();
        printTokenLinkStatus("Token B" , tokenBLinkCount);
        tokenB = null;
    }

    private void printTokenLinkStatus(String name, AtomicInteger linkCount) {
        int count = linkCount.incrementAndGet();
        if (count % 100 == 0) {
            System.out.println(name + " linked " + count + "/1000 times");
        }
    }
}
