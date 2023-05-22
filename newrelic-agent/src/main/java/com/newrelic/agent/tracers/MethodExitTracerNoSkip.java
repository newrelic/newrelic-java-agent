/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.attributes.AttributeValidator;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.trace.TransactionSegment;

import java.util.Collections;
import java.util.Map;

/**
 * A very lightweight tracer used to add code to the exit of a method invocation. It does not generate metrics and it
 * doesn't participate in transaction trace (or even the {@link Transaction} tracer stack).
 * <p>
 * These methods {@link #getDuration()}, {@link #getStartTime()} and {@link #getEndTime()} do not return accurate time
 * information as this tracer is not meant to measure invocation times.
 */
public abstract class MethodExitTracerNoSkip extends AbstractTracer {

    private final ClassMethodSignature signature;
    protected Tracer parentTracer;

    protected abstract void doFinish(int opcode, Object returnValue);

    /**
     * Create from transaction. The transaction must not be null, because it will be dereferenced in the superclass
     * constructor to grab the {@link TransactionActivity}
     *
     * @param signature
     * @param transaction
     */
    public MethodExitTracerNoSkip(ClassMethodSignature signature, Transaction transaction) {
        super(transaction);
        this.signature = signature;
        this.parentTracer = transaction.getTransactionActivity().getLastTracer();
    }

    /**
     * Create from TransactionActivity. For the benefit of legacy Play instrumentation, the activity may be null.
     *
     * @param signature
     * @param activity
     */
    public MethodExitTracerNoSkip(ClassMethodSignature signature, TransactionActivity activity) {
        super(activity, new AttributeValidator(ATTRIBUTE_TYPE));
        this.signature = signature;
        this.parentTracer = activity == null ? null : activity.getLastTracer();
    }

    @Override
    public void childTracerFinished(Tracer child) {
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public final void finish(int opcode, Object returnValue) {
        try {
            doFinish(opcode, returnValue);
        } finally {
            if (getTransaction() != null) {
                getTransaction().getTransactionActivity().tracerFinished(this, opcode);
            }
        }
    }

    @Override
    public void finish(Throwable throwable) {
    }

    @Override
    public Tracer getParentTracer() {
        return parentTracer;
    }

    @Override
    public void setParentTracer(Tracer tracer) {
        parentTracer = tracer;
    }

    @Override
    public final ClassMethodSignature getClassMethodSignature() {
        return signature;
    }

    @Override
    public final long getDurationInMilliseconds() {
        return 0;
    }

    @Override
    public final long getRunningDurationInNanos() {
        return 0;
    }

    @Override
    public final long getDuration() {
        return 0;
    }

    @Override
    public final long getExclusiveDuration() {
        return 0;
    }

    @Override
    public final long getStartTime() {
        return 0;
    }

    @Override
    public final long getStartTimeInMilliseconds() {
        return 0;
    }

    @Override
    public final long getEndTime() {
        return 0;
    }

    @Override
    public final long getEndTimeInMilliseconds() {
        return 0;
    }

    @Override
    public final String getMetricName() {
        return null;
    }

    @Override
    public String getTransactionSegmentName() {
        return null;
    }

    @Override
    public String getTransactionSegmentUri() {
        return null;
    }

    @Override
    public final Map<String, Object> getAgentAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Object getAgentAttribute(String key) {
        return null;
    }

    @Override
    public void setAgentAttribute(String key, Object value) {
        // this is never used in exit tracers
    }

    @Override
    public final boolean isTransactionSegment() {
        return false;
    }

    @Override
    public void removeAgentAttribute(String key) {

    }

    @Override
    public final boolean isMetricProducer() {
        return false;
    }

    @Override
    public boolean isParent() {
        return false;
    }

    @Override
    public void removeTransactionSegment() {

    }

    @Override
    public final TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig,
                                                          SqlObfuscator sqlObfuscator, long startTime, TransactionSegment lastSibling) {
        return new TransactionSegment(ttConfig, sqlObfuscator, startTime, this);
    }

    @Override
    public void setMetricName(String... metricNameParts) {
    }

    @Override
    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName, String transactionSegmentUri) {
    }

    @Override
    public String getGuid() {
        return null;
    }
}
