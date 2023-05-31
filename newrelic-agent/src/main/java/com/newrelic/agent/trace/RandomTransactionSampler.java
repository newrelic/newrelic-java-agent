/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.service.ServiceFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * This class is thread-safe.
 */
public class RandomTransactionSampler implements ITransactionSampler {

    private static final TransactionData FINISHED = new TransactionData(null, 0);
    // private static final TransactionData FINISHED = new TransactionData(null, null, 0, null, null, null, null, false,
    // 0, null, 0, 0, null, null, 0L, null, null, null, false);

    private final int maxTraces;
    private final AtomicReference<TransactionData> expensiveTransaction = new AtomicReference<>();
    private int tracesSent; // no synchronization needed - used only by the harvest thread

    protected RandomTransactionSampler(int maxTraces) {
        this.maxTraces = maxTraces;
    }

    @Override
    public boolean noticeTransaction(TransactionData td) {
        if (expensiveTransaction.compareAndSet(null, td)) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Captured random transaction trace for {0} {1}",
                        td.getApplicationName(), td);
                Agent.LOG.finer(msg);
            }
            return true;
        }
        return false;
    }

    @Override
    public List<TransactionTrace> harvest(String appName) {
        TransactionData td = expensiveTransaction.get();
        if (td == FINISHED) {
            return Collections.emptyList();
        }
        if (td == null) {
            return Collections.emptyList();
        }
        if (!Objects.equals(td.getApplicationName(), appName)) {
            return Collections.emptyList();
        }
        if (shouldFinish()) {
            td = expensiveTransaction.getAndSet(FINISHED);
            stop();
        } else {
            td = expensiveTransaction.getAndSet(null);
        }
        tracesSent++;
        return getTransactionTrace(td);
    }

    private List<TransactionTrace> getTransactionTrace(TransactionData td) {
        TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Sending random transaction trace for {0}: {1}", td.getApplicationName(),
                    td);
            Agent.LOG.finer(msg);
        }
        List<TransactionTrace> traces = new ArrayList<>(1);
        traces.add(trace);
        return traces;
    }

    private boolean shouldFinish() {
        return tracesSent >= maxTraces;
    }

    @Override
    public void stop() {
        ServiceFactory.getTransactionTraceService().removeTransactionTraceSampler(this);
        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Stopped random transaction tracing: max traces={1}", maxTraces);
            Agent.LOG.finer(msg);
        }
    }

    private void start() {
        ServiceFactory.getTransactionTraceService().addTransactionTraceSampler(this);
        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Started random transaction tracing: max traces={1}", maxTraces);
            Agent.LOG.finer(msg);
        }
    }

    public static RandomTransactionSampler startSampler(int maxTraces) {
        RandomTransactionSampler transactionSampler = new RandomTransactionSampler(maxTraces);
        transactionSampler.start();
        return transactionSampler;
    }

}
