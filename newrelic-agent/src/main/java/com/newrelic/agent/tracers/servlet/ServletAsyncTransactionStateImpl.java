/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.servlet;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionStateImpl;
import com.newrelic.agent.tracers.AbstractTracer;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import org.objectweb.asm.Opcodes;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class handles Servlet 3.0 and Jetty Continuations asynchronous processing.
 */
public class ServletAsyncTransactionStateImpl extends TransactionStateImpl {

    private static final ClassMethodSignature ASYNC_PROCESSING_SIG = new ClassMethodSignature(
            "NR_RECORD_ASYNC_PROCESSING_CLASS", "NR_RECORD_ASYNC_PROCESSING_METHOD", "()V");
    private static final MetricNameFormat ASYNC_PROCESSING_FORMAT = new SimpleMetricNameFormat("AsyncProcessing");
    private static final TracerFactory ASYNC_TRACER_FACTORY = new AsyncTracerFactory();

    private final Transaction transaction;
    private final AtomicReference<State> state = new AtomicReference<>(State.RUNNING);
    private volatile Tracer rootTracer;
    private volatile AbstractTracer asyncProcessingTracer;

    public ServletAsyncTransactionStateImpl(Transaction tx) {
        this.transaction = tx;
    }

    @Override
    public Tracer getTracer(Transaction tx, TracerFactory tracerFactory, ClassMethodSignature signature, Object object,
            Object... args) {
        if (state.compareAndSet(State.RESUMING, State.RUNNING)) {
            Tracer tracer = resumeRootTracer();
            if (tracer != null) {
                return tracer;
            }
        }
        return super.getTracer(tx, tracerFactory, signature, object, args);
    }

    @Override
    public Tracer getRootTracer() {
        if (state.compareAndSet(State.RESUMING, State.RUNNING)) {
            return resumeRootTracer();
        }
        return null;
    }

    @Override
    /**
     * Call this method when an asynchronous operation is dispatched.
     */
    public void resume() {
        if (!state.compareAndSet(State.SUSPENDING, State.RESUMING)) {
            return;
        }
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.finer(MessageFormat.format("Resuming transaction {0}", transaction));
        }
        Transaction.clearTransaction();
        Transaction.setTransaction(transaction);
    }

    @Override
    /**
     * Call this method when an asynchronous operation is started.
     */
    public void suspendRootTracer() {
        Transaction currentTx = Transaction.getTransaction(false);
        if (transaction != currentTx) {
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.finer(MessageFormat.format(
                        "Unable to suspend transaction {0} because it is not the current transaction {1}", transaction,
                        currentTx));
            }
            return;
        }
        if (!state.compareAndSet(State.RUNNING, State.SUSPENDING)) {
            return;
        }
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.finer(MessageFormat.format("Transaction {0} is suspended", transaction));
        }
    }

    @Override
    /**
     * Call this method when an asynchronous operation is complete.
     */
    public void complete() {
        if (!state.compareAndSet(State.SUSPENDING, State.RUNNING)) {
            return;
        }
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.finer(MessageFormat.format("Completing transaction {0}", transaction));
        }
        Transaction currentTx = Transaction.getTransaction(false); // currentTx may be null
        if (currentTx != transaction) {
            Transaction.clearTransaction();
            Transaction.setTransaction(transaction);
        }
        try {
            Tracer tracer = resumeRootTracer();
            if (tracer != null) {
                tracer.finish(Opcodes.ARETURN, null);
            }
        } finally {
            if (currentTx != transaction) {
                Transaction.clearTransaction();
                if (currentTx != null) { // JAVA-2644
                    Transaction.setTransaction(currentTx);
                }
            }
        }
    }

    @Override
    public boolean finish(Transaction tx, Tracer tracer) {
        if (state.get() == State.SUSPENDING && tracer == tx.getRootTracer()) {
            suspendRootTracer(tx, tx.getRootTracer());
            return false;
        } else {
            return true;
        }
    }

    private void suspendRootTracer(Transaction tx, Tracer tracer) {
        rootTracer = tracer;
        startAsyncProcessingTracer(tx);
        Transaction.clearTransaction();
    }

    /**
     * A tracer to record the time spent in asynchronous processing.
     */
    private void startAsyncProcessingTracer(Transaction tx) {
        if (asyncProcessingTracer == null) {
            asyncProcessingTracer = (AbstractTracer) super.getTracer(tx, ASYNC_TRACER_FACTORY, ASYNC_PROCESSING_SIG,
                    null, (Object[]) null);
        }
    }

    private Tracer resumeRootTracer() {
        stopAsyncProcessingTracer();
        Tracer tracer = rootTracer;
        rootTracer = null;
        return tracer;
    }

    private void stopAsyncProcessingTracer() {
        if (asyncProcessingTracer != null) {
            asyncProcessingTracer.finish(Opcodes.ARETURN, null);
        }
        asyncProcessingTracer = null;
    }

    private enum State {

        RESUMING, RUNNING, SUSPENDING
    }

    private static class AsyncTracerFactory extends AbstractTracerFactory {

        @Override
        public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
            return new DefaultTracer(tx, sig, object, ASYNC_PROCESSING_FORMAT);
        }

    }
}
