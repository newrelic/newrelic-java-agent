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
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormats;

public class OtherRootSqlTracer extends DefaultSqlTracer implements TransactionActivityInitiator {

    private final MetricNameFormat uri;

    public OtherRootSqlTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter) {
        this(transaction.getTransactionActivity(), sig, object, metricNameFormatter);
    }

    public OtherRootSqlTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter, int tracerFlags, long time) {
        super(transaction, sig, object, metricNameFormatter, tracerFlags, time);
        this.uri = metricNameFormatter;
    }

    public OtherRootSqlTracer(TransactionActivity txa, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter) {
        super(txa, sig, object, metricNameFormatter, DEFAULT_TRACER_FLAGS);
        this.uri = metricNameFormatter;
    }

    public OtherRootSqlTracer(TransactionActivity txa, ClassMethodSignature sig, Object target, MetricNameFormat uri,
            int flags) {
        super(txa, sig, target, uri, TracerFlags.forceMandatoryRootFlags(flags), System.nanoTime());
        this.uri = uri;
    }

    public OtherRootSqlTracer(TransactionActivity txa, ClassMethodSignature sig, Object target, MetricNameFormat uri,
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
        Transaction transaction = getTransaction();
        if (transaction != null && this.equals(transaction.getTransactionActivity().getRootTracer())) {
            transaction.setThrowable(throwable, TransactionErrorPriority.TRACER, false);
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
