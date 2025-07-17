/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.dispatchers.OtherDispatcher;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormats;

public class OtherRootTracer extends DefaultTracer implements TransactionActivityInitiator {

    // Tracers MUST NOT store references to the Transaction. Why: tracers are stored in the TransactionActivity,
    // and Activities can be reparented from one Transaction to another by the public APIs that support async.

    private final MetricNameFormat uri;

    /**
     * Create tracer for current Activity of Transaction.
     * 
     * @param transaction the transaction. Must not be null because it is dereferenced to get the Activity.
     * @param sig
     * @param object
     * @param uri
     */
    public OtherRootTracer(Transaction transaction, ClassMethodSignature sig, Object object, MetricNameFormat uri) {
        this(transaction.getTransactionActivity(), sig, object, uri);
    }

    /**
     * Create tracer for activity.
     * 
     * @param txa the activity. For the benefit of legacy Play instrumentation and unit tests, may be null.
     * @param sig
     * @param target
     * @param uri
     */
    public OtherRootTracer(TransactionActivity txa, ClassMethodSignature sig, Object target, MetricNameFormat uri) {
        this(txa, sig, target, uri, DEFAULT_TRACER_FLAGS, System.nanoTime());
    }

    public OtherRootTracer(TransactionActivity txa, ClassMethodSignature sig, Object target, MetricNameFormat uri,
            int flags) {
        super(txa, sig, target, uri, TracerFlags.forceMandatoryRootFlags(flags), System.nanoTime());
        this.uri = uri;
    }

    /**
     * Create tracer for activity.
     *
     * @param txa the activity. For the benefit of legacy Play instrumentation and unit tests, may be null.
     * @param sig
     * @param target
     * @param uri
     */
    public OtherRootTracer(TransactionActivity txa, ClassMethodSignature sig, Object target, MetricNameFormat uri,
            int flags, long pStartTime) {
        super(txa, sig, target, MetricNameFormats.getFormatter(target, sig), TracerFlags.forceMandatoryRootFlags(flags),
                pStartTime);
        this.uri = uri;
    }

    @Override
    public Dispatcher createDispatcher() {
        return new OtherDispatcher(getTransaction(), uri);
    }

    @Override
    protected void doFinish(Throwable throwable) {
        super.doFinish(throwable);
        if (this.equals(getTransaction().getTransactionActivity().getRootTracer())) {
            getTransaction().setThrowable(throwable, TransactionErrorPriority.TRACER, false);
        }
    }

    /***
     * This API method allows leaves to be excluded outside the constructor in DefaultTracer.
     * For now, we don't want to give this capability to root tracers. The existing agent pattern is to force
     * root tracers to be included (see TracerFlags.forceMandatoryRootFlags, which ignores excludeFromTransactionTrace).
     *
     * This can be reevaluated in the future if we find a use case for excluding root tracers.
     */

    @Override
    public void excludeLeaf() {}

}
