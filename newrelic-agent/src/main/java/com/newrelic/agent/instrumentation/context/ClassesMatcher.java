/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

public class ClassesMatcher {

    /**
     * This parallelizes matching of a large number of classes by firing up threads to handle
     * a partition of the classes, capped at {@code maxThreads}, e.g. a value sourced from
     * {@link com.newrelic.agent.config.ClassTransformerConfig#getMaxWeaveStartupThreads()}.
     *
     * If 10 classes are passed in and the cap is 5, it will create 5 threads.
     * If 100 classes are passed in and the cap is 8, it will create 8 threads.
     */
    public static Set<Class<?>> getMatchingClasses(final Collection<ClassMatchVisitorFactory> matchers,
                                                   final InstrumentationContextClassMatcherHelper matchHelper,
                                                   int maxThreads,
                                                   Class<?>... classes) {
        final Set<Class<?>> matchingClasses = Sets.newConcurrentHashSet();
        if (classes == null || classes.length == 0) {
            return matchingClasses;
        }

        double partitions = Math.min(classes.length, maxThreads);
        int estimatedPerPartition = (int) Math.ceil(classes.length / partitions);
        List<List<Class<?>>> partitionsClasses = Lists.partition(Arrays.asList(classes), estimatedPerPartition);

        final CountDownLatch countDownLatch = new CountDownLatch(partitionsClasses.size());
        for (final List<Class<?>> partitionClasses : partitionsClasses) {
            Runnable matchingRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        for (Class<?> clazz : partitionClasses) {
                            if (matchHelper.isMatch(matchers, clazz)) {
                                matchingClasses.add(clazz);
                            }
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            new Thread(matchingRunnable).start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Agent.LOG.log(Level.INFO, "Failed to wait for matching classes");
            Agent.LOG.log(Level.FINER, e, "Interrupted during class matching");
        }

        return matchingClasses;
    }
}
