/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.service.ServiceFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * This class is thread-safe.
 */
public class RandomTransactionSampler implements ITransactionSampler {

    private static final TransactionData FINISHED = new TransactionData(null, 0);

    private final int maxTraces;
    private final AtomicReference<TransactionData> expensiveTransaction = new AtomicReference<>();
    private int tracesSent; // no synchronization needed - used only by the harvest thread

    private final TransactionTraceCollector transactionTraceCollector;

    protected RandomTransactionSampler(int maxTraces, TransactionTraceCollector transactionTraceCollector) {
        this.maxTraces = maxTraces;
        this.transactionTraceCollector = transactionTraceCollector;
    }

    @Override
    public boolean noticeTransaction(TransactionData td) {
        if (expensiveTransaction.compareAndSet(null, td)) {
            // set transaction trace attribute on new expensive transaction
            markAsTransactionTraceCandidate(td);

            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Captured random transaction trace for {0} {1}",
                        td.getApplicationName(), td);
                Agent.LOG.finer(msg);
            }
            return true;
        }
        return false;
    }

    /**
     * When using Span based transaction traces, this will add a transaction_trace attribute to
     * indicate that the span belongs to a trace that was a transaction trace candidate.
     * <p>
     * It will also increase the priority for each subsequent transaction trace candidate to guarantee
     * that the most expensive transactions will have their spans sampled.
     *
     * @param td TransactionData for finished transaction
     */
    private void markAsTransactionTraceCandidate(TransactionData td) {
        if (ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig().getTransactionTracesAsSpans()) {
            if (td != null) {
                Map<String, Object> intrinsicAttributes = td.getIntrinsicAttributes();
                if (intrinsicAttributes != null) {
                    intrinsicAttributes.put(AttributeNames.TRANSACTION_TRACE, true);
                }
            }
        }
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

        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Sending random transaction trace for {0}: {1}", td.getApplicationName(),
                    td);
            Agent.LOG.finer(msg);
        }

        if (!ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig().getTransactionTracesAsSpans()) {
            // Construct TransactionTrace as JSON blob
            return getTransactionTrace(td);
        } else {
            // TODO anything to do here for span based TTs?
            return Collections.emptyList();
        }
    }

    private List<TransactionTrace> getTransactionTrace(TransactionData td) {
        TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
        List<TransactionTrace> traces = new ArrayList<>(1);
        traces.add(trace);
        return traces;
    }

    /**
     * We might capture 1 random TT per harvest. Once 5 random TTs have been captured we shut
     * this sampler down. See TransactionTraceCollector.INITIAL_TRACE_LIMIT.
     *
     * @return true if no more random transactions should be sampled, else false
     */
    private boolean shouldFinish() {
        return tracesSent >= maxTraces;
    }

    @Override
    public void stop() {
        transactionTraceCollector.removeTransactionTraceSampler(this);

        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Stopped random transaction tracing: max traces={1}", maxTraces);
            Agent.LOG.finer(msg);
        }
    }
}
