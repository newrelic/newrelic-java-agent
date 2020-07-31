/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.newrelic.agent.Agent;
import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * This class is not thread-safe. Use only on the sampling thread, except for the dispatcherTransactionFinished method
 * which runs on a request thread.
 */
public class KeyTransactionProfile implements IProfile, ExtendedTransactionListener, JSONStreamAware {

    private static final int CAPACITY = 100;

    private final IProfile delegate;

    private final String keyTransaction;
    private final Map<Long, BlockingQueue<StackTraceHolder>> pendingStackTraces = new ConcurrentHashMap<>();
    private final Queue<StackTraceHolder> releasedStackTraces = new ConcurrentLinkedQueue<>();
    private final Multiset<Long> activeTransactionThreadIds = ConcurrentHashMultiset.create();

    public KeyTransactionProfile(Profile profile) {
        super();
        this.keyTransaction = profile.getProfilerParameters().getKeyTransaction();
        this.delegate = profile;
    }

    /**
     * For testing.
     */
    IProfile getDelegate() {
        return delegate;
    }

    @Override
    public void start() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
        delegate.start();
    }

    @Override
    public void end() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
        pendingStackTraces.clear();
        releaseStackTraces();
        delegate.end();
        activeTransactionThreadIds.clear();
    }

    @Override
    public ProfilerParameters getProfilerParameters() {
        return delegate.getProfilerParameters();
    }

    @Override
    public int getSampleCount() {
        return delegate.getSampleCount();
    }

    @Override
    public Long getProfileId() {
        return delegate.getProfileId();
    }

    @Override
    public ProfileTree getProfileTree(ThreadType threadType) {
        return delegate.getProfileTree(threadType);
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        delegate.writeJSONString(out);
    }

    @Override
    public int trimBy(int count) {
        return delegate.trimBy(count);
    }

    @Override
    public long getStartTimeMillis() {
        return delegate.getStartTimeMillis();
    }

    @Override
    public long getEndTimeMillis() {
        return delegate.getEndTimeMillis();
    }

    /**
     * For testing.
     */
    Map<Long, Integer> getPendingThreadQueueSizes() {
        Map<Long, Integer> pendingThreadQueueSizes = new HashMap<>();
        for (Map.Entry<Long, BlockingQueue<StackTraceHolder>> entry : pendingStackTraces.entrySet()) {
            pendingThreadQueueSizes.put(entry.getKey(), entry.getValue().size());
        }
        return pendingThreadQueueSizes;
    }

    /**
     * For testability. Despite what the guava documentation states, this is a read-only view of the collection.
     * Calling add will cause an UnsupportedOperationException.
     */
    Set<Long> getActiveThreadIds() {
        return activeTransactionThreadIds.elementSet();
    }

    private void releaseStackTraces() {
        while (true) {
            StackTraceHolder holder = releasedStackTraces.poll();
            if (holder == null) {
                return;
            }
            delegate.addStackTrace(holder.getThreadId(), holder.isRunnable(), holder.getType(), holder.getStackTrace());
        }
    }

    @Override
    public void dispatcherTransactionStarted(Transaction transaction) {
        activeTransactionThreadIds.add(transaction.getInitiatingThreadId());
    }

    /**
     * This usually runs on the request thread, but for transactions that end because of a token timing out, it can be
     * called from an arbitrary thread that happens to run the onRemoval listener in TimedTokenSet.
     */
    @Override
    public void dispatcherTransactionFinished(TransactionData td, TransactionStats stats) {
        try {
            doDispatcherTransactionFinished(td, stats);
        } catch (Exception e) {
            String msg = MessageFormat.format("Error releasing stack traces for \"{0}\": {1}", td.getBlameMetricName(),
                    e);
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, e);
            } else {
                Agent.LOG.finer(msg);
            }
        } finally {
            long threadId = td.getTransaction().getInitiatingThreadId();
            boolean success = getActiveThreadIds().remove(threadId);
            Agent.LOG.finer("Attempt to remove thread id: " + threadId + " was " + (success ? "" : "not ") + "successful");
        }
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
        getActiveThreadIds().remove(transaction.getInitiatingThreadId());
    }

    private void doDispatcherTransactionFinished(TransactionData td, TransactionStats stats) {
        boolean isKeyTransaction = isKeyTransaction(td);

        for (TransactionActivity transactionActivity : td.getTransactionActivities()) {
            BlockingQueue<StackTraceHolder> holderQueue = getHolderQueue(transactionActivity.getThreadId());
            if (holderQueue == null) {
                continue;
            }

            long startTime = transactionActivity.getRootTracer().getStartTime();
            long endTime = transactionActivity.getRootTracer().getEndTime();

            Collection<StackTraceHolder> holders = new ArrayList<>(holderQueue.size());
            holderQueue.drainTo(holders);
            for (StackTraceHolder holder : holders) {
                if (holder == null) {
                    break;
                }
                if (startTime > holder.getStackTraceTime() || endTime < holder.getStackTraceTime()) {
                    holderQueue.offer(holder);
                    continue;
                }
                if (isKeyTransaction) {
                    releasedStackTraces.add(holder);
                }
            }
        }
    }

    private boolean isKeyTransaction(TransactionData td) {
        return keyTransaction.equals(td.getBlameMetricName());
    }

    @Override
    public void beforeSampling() {
        clearNonTransactionQueues();
        releaseStackTraces();
        delegate.beforeSampling();
    }

    /**
     * Grab the thread ids for all current transactions in the system and attempt to remove anything not in that
     * set. This will help prevent memory growth in the case where there are frequent stack traces being taken
     * on threads that are not part of any transactions.
     */
    private void clearNonTransactionQueues() {
        Set<Long> pendingThreadIds = pendingStackTraces.keySet();

        // This will modify the underlying pendingStackTraces map by removing
        // all thread ids that don't map to an active transaction
        pendingThreadIds.retainAll(getActiveThreadIds());
    }

    @Override
    public void addStackTrace(long threadId, boolean runnable, ThreadType type, StackTraceElement... stackTrace) {
        if (type != ThreadType.BasicThreadType.OTHER) {
            return;
        }
        StackTraceHolder holder = new StackTraceHolder(threadId, runnable, type, stackTrace);
        Queue<StackTraceHolder> holderQueue = getOrCreateHolderQueue(threadId);
        holderQueue.offer(holder);
    }

    private BlockingQueue<StackTraceHolder> getHolderQueue(long threadId) {
        return pendingStackTraces.get(threadId);
    }

    private BlockingQueue<StackTraceHolder> getOrCreateHolderQueue(long threadId) {
        BlockingQueue<StackTraceHolder> holderQueue = pendingStackTraces.get(threadId);
        if (holderQueue == null) {
            holderQueue = new LinkedBlockingQueue<>(CAPACITY);
            pendingStackTraces.put(threadId, holderQueue);
        }
        return holderQueue;
    }

    private static class StackTraceHolder {

        private final long threadId;
        private final boolean runnable;
        private final ThreadType type;
        private final long stackTraceTime;
        private final StackTraceElement[] stackTrace;

        private StackTraceHolder(long threadId, boolean runnable, ThreadType type, StackTraceElement... stackTrace) {
            this.threadId = threadId;
            this.runnable = runnable;
            this.type = type;
            this.stackTrace = stackTrace;
            stackTraceTime = System.nanoTime();
        }

        public long getThreadId() {
            return threadId;
        }

        public boolean isRunnable() {
            return runnable;
        }

        public ThreadType getType() {
            return type;
        }

        public StackTraceElement[] getStackTrace() {
            return stackTrace;
        }

        public long getStackTraceTime() {
            return stackTraceTime;
        }

    }

    /**
     * Key transactions profiles should not mark instrumented methods.
     */
    @Override
    public void markInstrumentedMethods() {
        // do nothing
    }

}
