/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.Collections;
import java.util.List;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.TransactionCache;

public class MockTransactionActivity extends TransactionActivity {
    /**
     * Clear the TransactionActivity from the thread local that holds it. This is a "dangerous" interface that is
     * required for instrumentation in which the transaction+activity are multiplexed on a thread (e.g. "coroutine" or
     * "continuation" mechanisms such as Javaflow or the async servlet interface).
     */
    public static void clear() {
    }

    /**
     * Jam the argument into the thread local that holds it. This is a "dangerous" interface that is required for
     * instrumentation in which the transaction+activity are multiplexed on a thread (e.g. "coroutine" or "continuation"
     * mechanisms such as Javaflow or the async servlet interface).
     * 
     * @param txa the new value to make current
     */
    public static void set(TransactionActivity txa) {
    }

    public static TransactionActivity get() {
        return null;
    }

    public static TransactionActivity create(Transaction transaction) {
        return new MockTransactionActivity();
    }

    @Override
    public TransactionStats getTransactionStats() {
        return null;
    }

    /**
     * Return the current state of the tracer stack for this activity.
     * 
     * @return the current state of the tracer stack for this activity.
     */
    @Override
    public List<Tracer> getTracers() {
        return Collections.unmodifiableList(super.getTracers());
    }

    @Override
    public long getTotalCpuTime() {
        return 0L;
    }

    /**
     * This should really only be called from the transaction because this only handles the Transaction Activity logic
     * and not the transaction logic.
     */
    @Override
    public void setToIgnore() {
    }

    /**
     * Adds a tracer to the call stack.
     * 
     * @param tracer
     */
    @Override
    public Tracer tracerStarted(Tracer tracer) {
        return null;
    }

    /**
     * Pop the finished tracer off the call stack.
     */
    @Override
    public void tracerFinished(Tracer tracer, int opcode) {
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void recordCpu() {
    }

    @Override
    public void addTracer(Tracer tracer) {
    }

    /**
     * Get the last tracer on the call stack, or null if the call stack is empty.
     */
    @Override
    public Tracer getLastTracer() {
        return null;
    }

    @Override
    public TracedMethod startFlyweightTracer() {
        return null;
    }

    @Override
    public void finishFlyweightTracer(TracedMethod parent, long startInNanos, long finishInNanos, String className,
            String methodName, String methodDesc, String metricName, String[] rollupMetricNames) {
    }

    @Override
    public void startAsyncActivity(Transaction transaction, int activityId, Tracer parentTracer) {
    }

    @Override
    public Tracer getRootTracer() {
        return null;
    }

    /**
     * Get a cache to store objects for the life of the transaction.
     */
    @Override
    public TransactionCache getTransactionCache() {
        return null;
    }

    @Override
    public Transaction getTransaction() {
        return null;
    }
}
