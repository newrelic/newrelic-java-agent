/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultSqlTracer;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootSqlTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.SkipTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.UltraLightTracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormats;

import java.util.logging.Level;

/**
 * Ironically, given its name, this class is threadsafe because it holds no state.
 */
public class TransactionStateImpl implements TransactionState {

    @Override
    public Tracer getTracer(Transaction tx, TracerFactory tracerFactory, ClassMethodSignature sig, Object obj, Object... args) {
        TransactionActivity activity = tx.getTransactionActivity();
        if (tx.isIgnore() || activity.isTracerStartLocked()) {
            return null;
        }

        Tracer tracer = tracerFactory.getTracer(tx, sig, obj, args);
        return tracerStarted(tx, sig, tracer);
    }

    private boolean currentlyExceedingSegmentLimit(Transaction tx, ClassMethodSignature sig) {
        if (tx.getTransactionCounts().isOverTracerSegmentLimit()) {
            Agent.LOG.log(Level.FINEST, "Transaction has exceeded tracer segment limit. " +
                            "Returning Ultralight tracer. Transaction name: {0} Traced method signature: {1}",
                    tx.getPriorityTransactionName(), sig);
            return true;
        }
        return false;
    }

    @Override
    public Tracer getTracer(Transaction tx, String tracerFactoryName, ClassMethodSignature sig, Object obj, Object... args) {
        TracerFactory tracerFactory = ServiceFactory.getTracerService().getTracerFactory(tracerFactoryName);
        return getTracer(tx, tracerFactory, sig, obj, args);
    }

    /**
     * Get tracer for weaved code and XML instrumentation (no tracer factory)
     */
    @Override
    public Tracer getTracer(Transaction tx, final Object invocationTarget, final ClassMethodSignature sig, final String metricName, final int flags) {
        TransactionActivity activity = tx.getTransactionActivity();
        if (tx.isIgnore() || activity.isTracerStartLocked()) {
            return null;
        }

        Tracer tracer;
        final MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);

        if (TracerFlags.isDispatcher(flags)) {
            tracer = new OtherRootTracer(tx, sig, invocationTarget, mnf);
        } else {
            tracer = new DefaultTracer(tx, sig, invocationTarget, mnf, flags);
        }
        return tracerStarted(tx, sig, tracer);
    }

    @Override
    public Tracer getSqlTracer(Transaction tx, Object invocationTarget, ClassMethodSignature sig, String metricName, int flags) {
        TransactionActivity activity = tx.getTransactionActivity();
        if (tx.isIgnore() || activity.isTracerStartLocked()) {
            return null;
        }

        if (currentlyExceedingSegmentLimit(tx, sig)) {
            return UltraLightTracer.createClampedSegment(activity, sig);
        }

        Tracer tracer;
        final MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);

        if (TracerFlags.isDispatcher(flags)) {
            tracer = new OtherRootSqlTracer(tx, sig, invocationTarget, mnf);
        } else {
            tracer = new DefaultSqlTracer(tx, sig, invocationTarget, mnf, flags);
        }

        return tracerStarted(tx, sig, tracer);
    }

    private Tracer tracerStarted(Transaction tx, final ClassMethodSignature sig, Tracer tracer) {
        if (tracer == null || tracer instanceof SkipTracer) {
            return tracer;
        }

        tracer = tx.getTransactionActivity().tracerStarted(tracer);
        if (tracer != null && Agent.LOG.isLoggable(Level.FINER)) {
            if (tracer == tx.getRootTracer()) {
                Agent.LOG.log(Level.FINER, "Transaction started {0}", tx);
            }
            Agent.LOG.log(Level.FINER, "Tracer ({3}) Started: {0}.{1}{2}", sig.getClassName(), sig.getMethodName(), sig.getMethodDesc(), tracer);
        }

        return tracer;
    }

    @Override
    public Tracer getRootTracer() {
        return null;
    }

    @Override
    public void resume() {
    }

    @Override
    public void suspend() {
    }

    @Override
    public void complete() {
    }

    @Override
    public boolean finish(Transaction tx, Tracer tracer) {
        return true;
    }

    @Override
    public void suspendRootTracer() {
    }

}
