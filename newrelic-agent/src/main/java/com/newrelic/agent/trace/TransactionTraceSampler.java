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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * This class is thread-safe.
 */
public class TransactionTraceSampler implements ITransactionSampler {

    private static final int NO_TRACE_LIMIT = 5;

    private volatile Map<String, Long> tracedTransactions;
    private final AtomicReference<TransactionData> expensiveTransaction = new AtomicReference<>();
    private final int topN;
    private int noTraceCount; // no synchronization needed - used only by the harvest thread.
    private final Lock readLock;
    private final Lock writeLock;

    public TransactionTraceSampler() {
        topN = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig().getTopN();
        tracedTransactions = Collections.unmodifiableMap(new HashMap<String, Long>(topN));
        ReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    @Override
    public boolean noticeTransaction(TransactionData td) {
        if (!exceedsThreshold(td)) {
            return false;
        }

        TransactionData maxTrace = expensiveTransaction.get();
        if (maxTrace != null && getScore(maxTrace) >= getScore(td)) {
            return false;
        }

        readLock.lock();
        try {
            return noticeTransactionUnderLock(td);
        } finally {
            readLock.unlock();
        }
    }

    private boolean noticeTransactionUnderLock(TransactionData td) {
        Long lastScore = tracedTransactions.get(td.getBlameMetricName());
        if (lastScore != null && getScore(td) <= lastScore) {
            return false;
        }
        while (true) {
            TransactionData current = expensiveTransaction.get();
            if (current != null && getScore(current) >= getScore(td)) {
                return false;
            }
            if (expensiveTransaction.compareAndSet(current, td)) {
                // reset transaction trace attribute on current expensive transaction
//                markAsTransactionTraceCandidate(current, false);
                // set transaction trace attribute on new expensive transaction
                markAsTransactionTraceCandidate(td, true);

                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat.format("Captured expensive transaction trace for {0} {1}",
                            td.getApplicationName(), td);
                    Agent.LOG.finer(msg);
                }
                return true;
            }
        }
    }

    private void markAsTransactionTraceCandidate(TransactionData td, boolean isTransactionTrace) {
        if (td != null) {
            Map<String, Object> intrinsicAttributes = td.getIntrinsicAttributes();
            if (intrinsicAttributes != null) {
                intrinsicAttributes.put(AttributeNames.TRANSACTION_TRACE, isTransactionTrace);

                Float priority = (Float) intrinsicAttributes.get(AttributeNames.PRIORITY);
                Float ttPriority = priority + 5;
                intrinsicAttributes.put(AttributeNames.PRIORITY, ttPriority);

                td.getTransaction().setPriorityIfNotNull(ttPriority);
            }
        }
    }

    /**
     * @param td TransactionData.
     * @return true if the transaction exceeds it's transaction trace threshold. false otherwise.
     */
    protected boolean exceedsThreshold(TransactionData td) {
        if (td.getLegacyDuration() > td.getTransactionTracerConfig().getTransactionThresholdInNanos()) {
           return true;
        }
        else {
            Agent.LOG.log(Level.FINER, "Transaction trace threshold not exceeded {0}", td);
            return false;
        }
    }

    /**
     * @param td
     * @return score used to determine most expensive transaction.
     */
    protected long getScore(TransactionData td) {
        return td.getLegacyDuration();
    }

    @Override
    public List<TransactionTrace> harvest(String appName) {
        TransactionData td = null;
        writeLock.lock();
        try {
            td = harvestUnderLock(appName);
        } finally {
            writeLock.unlock();
        }
        if (td == null) {
            return Collections.emptyList();
        }
        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Sending transaction trace for {0} {1}", td.getApplicationName(), td);
            Agent.LOG.finer(msg);
        }
        TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
        List<TransactionTrace> traces = new ArrayList<>(1);
        traces.add(trace);
        return traces;
    }

    private TransactionData harvestUnderLock(String appName) {
        TransactionData td = expensiveTransaction.getAndSet(null);

        if (topN == 0) {
            return td;
        }

        if (td == null) {
            checkAndClearTracedTransactions();
        } else {
            noticeTracedTransaction(td);
        }
        return td;
    }

    /*
     * Clear our map of traced transactions if we have no trace to report in NO_TRACE_LIMIT consecutive harvest cycles.
     */
    private void checkAndClearTracedTransactions() {
        noTraceCount++;
        if (noTraceCount >= NO_TRACE_LIMIT && !tracedTransactions.isEmpty()) {
            noTraceCount = 0;
            tracedTransactions = Collections.unmodifiableMap(new HashMap<String, Long>(topN));
        }
    }

    private void noticeTracedTransaction(TransactionData td) {
        noTraceCount = 0;

        Map<String, Long> ttMap = new HashMap<>(topN);
        if (tracedTransactions.size() < topN) {
            ttMap.putAll(tracedTransactions);
        }
        ttMap.put(td.getBlameMetricName(), getScore(td));
        tracedTransactions = Collections.unmodifiableMap(ttMap);
    }

    @Override
    public void stop() {
        expensiveTransaction.set(null);
        tracedTransactions = Collections.unmodifiableMap(new HashMap<String, Long>(topN));
    }

}
