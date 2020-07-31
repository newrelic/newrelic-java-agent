/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;

import java.lang.instrument.UnmodifiableClassException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Uses the SamplerService thread to periodically request retransform for classes added to the queue.
 */
public class PeriodicRetransformer implements Retransformer, Runnable {
    private static final int FREQUENCY_IN_SECONDS = 10;

    public static final Retransformer INSTANCE = new PeriodicRetransformer();

    private final AtomicReference<ConcurrentLinkedQueue<Class<?>>> classesToRetransform = new AtomicReference<>(new ConcurrentLinkedQueue<Class<?>>());
    private final AtomicBoolean scheduled = new AtomicBoolean(false);

    private PeriodicRetransformer() {
    }

    @Override
    public void run() {
        ConcurrentLinkedQueue<Class<?>> classList = classesToRetransform.getAndSet(new ConcurrentLinkedQueue<Class<?>>());
        if (classList.isEmpty()) {
            return;
        }
        Set<Class<?>> classSet = new HashSet<>(classList);
        try {
            ServiceFactory.getCoreService().getInstrumentation().retransformClasses(classSet.toArray(new Class[] {}));
        } catch (UnmodifiableClassException e) {
            Agent.LOG.fine(MessageFormat.format("Unable to retransform class: {0}", e.getMessage()));
        }
    }

    @Override
    public void queueRetransform(Set<Class<?>> classesToRetransform) {
        this.classesToRetransform.get().addAll(classesToRetransform);
        if (!scheduled.get() && !scheduled.getAndSet(true)) {
            ServiceFactory.getSamplerService().addSampler(this, FREQUENCY_IN_SECONDS, TimeUnit.SECONDS);
        }
    }
}