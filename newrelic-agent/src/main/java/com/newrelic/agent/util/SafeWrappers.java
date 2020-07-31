/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.newrelic.agent.Agent;

public class SafeWrappers {

    private SafeWrappers() {
    }

    public static Runnable safeRunnable(final Runnable runnable) {
        return new Runnable() {

            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    try {
                        Agent.LOG.log(Level.SEVERE, t.toString());
                        Agent.LOG.log(Level.FINEST, t, t.toString());

                    } catch (Throwable t2) {
                    }
                }

            }
        };
    }

    /**
     * This wraps a scheduled executor so that provided Runnables squelch and log all exceptions. Right
     * now the apis that use callables and all of the invokeAny / invokeAll methods are unsupported. The shutdown calls
     * are also unsupported so that services can pass out a wrapped reference to their executor without a caller being
     * able to shut it down.
     */
    public static ScheduledExecutorService safeExecutor(final ScheduledExecutorService executor) {
        return new ScheduledExecutorService() {

            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                return executor.schedule(safeRunnable(command), delay, unit);
            }

            public void execute(Runnable command) {
                executor.execute(command);
            }

            public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                    TimeUnit unit) {
                return executor.scheduleAtFixedRate(safeRunnable(command), initialDelay, period, unit);
            }

            public void shutdown() {
                throw new UnsupportedOperationException();
            }

            public List<Runnable> shutdownNow() {
                throw new UnsupportedOperationException();
            }

            public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                    TimeUnit unit) {
                return executor.scheduleWithFixedDelay(safeRunnable(command), initialDelay, delay, unit);
            }

            public boolean isShutdown() {
                return executor.isShutdown();
            }

            public boolean isTerminated() {
                return executor.isTerminated();
            }

            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            public <T> Future<T> submit(Callable<T> task) {
                throw new UnsupportedOperationException();
            }

            public <T> Future<T> submit(Runnable task, T result) {
                return executor.submit(safeRunnable(task), result);
            }

            public Future<?> submit(Runnable task) {
                return executor.submit(safeRunnable(task));
            }

            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                    throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
                    ExecutionException {
                throw new UnsupportedOperationException();
            }

            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                throw new UnsupportedOperationException();
            }
        };
    }
}
