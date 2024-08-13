/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.MapMaker;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.*;
import com.newrelic.agent.transaction.MergeStatsEngineResolvingScope;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.TimeConversion;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * This class tracks running transactions and notifies any registered listeners for the given transaction states:
 *
 * - Dispatcher Transaction starting
 * - Dispatcher Transaction error during finish
 * - Dispatcher Transaction finished
 *
 * This class is thread-safe.
 */
public class TransactionService extends AbstractService {

    private static final String TRANSACTION_SERVICE_PROCESSOR_THREAD_NAME = "New Relic Transaction Service Processor";

    // TODO priorityTransactionListeners should probably be a priority queue so entries can actually be sorted
    private final List<PriorityTransactionListener> priorityTransactionListeners = new CopyOnWriteArrayList<>();
    private final List<TransactionListener> transactionListeners = new CopyOnWriteArrayList<>();
    private final List<ExtendedTransactionListener> extendedTransactionListeners = new CopyOnWriteArrayList<>();
    private final List<TransactionStatsListener> transactionStatsListeners = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<Transaction, String> updateQueue;
    private final String placeholder = "placeholder";

    private AtomicLong txStartedThisHarvest = new AtomicLong(0);
    private AtomicLong txFinishedThisHarvest = new AtomicLong(0);
    private AtomicLong txCancelledThisHarvest = new AtomicLong(0);

    public TransactionService() {
        this(1, 5L, 30L, TimeUnit.SECONDS);
    }

    public TransactionService(int numMaintenanceThreads, long initialDelay, long delay, TimeUnit timeUnit) {
        super(TransactionService.class.getSimpleName());

        // prevent the update queue polling period from being less than 1 second
        long initialDelayMilli = TimeConversion.convertToMilliWithLowerBound(initialDelay, timeUnit, 1000L);
        long delayMilli = TimeConversion.convertToMilliWithLowerBound(delay, timeUnit, 1000L);

        // uses a map because there is no concurrent hash set, also does not permit null keys or values
        updateQueue = new MapMaker().concurrencyLevel(16).makeMap();

        // run as daemon to not prevent shutdown of application
        scheduler = Executors.newScheduledThreadPool(numMaintenanceThreads, new DefaultThreadFactory(TRANSACTION_SERVICE_PROCESSOR_THREAD_NAME, true));
        scheduler.scheduleWithFixedDelay(() -> TransactionService.this.processQueue(), initialDelayMilli, delayMilli, TimeUnit.MILLISECONDS);
    }

    /**
     * The only reason this was pulled out of the runnable is for testing purposes. Otherwise, do not call it directly.
     */
    public void processQueue() {
        try {
            int transactionCount = 0;
            for (Iterator<Transaction> txi = updateQueue.keySet().iterator(); txi.hasNext(); ) {
                txi.next().cleanUp();
                transactionCount++;
            }
            getLogger().finer("Transaction service processed " + transactionCount + " transactions");
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, t, "Exception processing async update queue.");
        }
    }

    public void transactionStarted(Transaction transaction) {
        if (transaction != null && isStarted()) {
            updateQueue.put(transaction, placeholder);
            txStartedThisHarvest.incrementAndGet();
            if (transaction.getDispatcher() != null) {
                for (ExtendedTransactionListener listener : extendedTransactionListeners) {
                    listener.dispatcherTransactionStarted(transaction);
                }
            }
        }
    }

    /**
     * A transaction completed "normally". This means the Agent completed its processing normally; the application
     * processing captured by the transaction may have resulted in an error.
     */
    public void transactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        try {
            // here we go
            doProcessTransaction(transactionData, transactionStats);
            txFinishedThisHarvest.incrementAndGet();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, e, "Error recording transaction \"{0}\"", transactionData.getBlameMetricName());
        } finally {
            updateQueue.remove(transactionData.getTransaction());
        }
    }

    /**
     * The transaction is completing abruptly, either because the agent encountered an internal error or because the
     * transaction's sole activity was reparented to another transaction as a result of token linking. Cancelled
     * transactions are not reported to New Relic and they do not generate transaction events.
     */
    public void transactionCancelled(Transaction transaction) {
        try {
            txCancelledThisHarvest.incrementAndGet();
            if (transaction.getDispatcher() != null) {
                for (ExtendedTransactionListener listener : extendedTransactionListeners) {
                    listener.dispatcherTransactionCancelled(transaction);
                }
            }
        } finally {
            updateQueue.remove(transaction); // reduces likelihood of JAVA-2647
        }
    }

    private void doProcessTransaction(TransactionData transactionData, TransactionStats transactionStats) {
        if (!ServiceFactory.getServiceManager().isStarted() || !ServiceFactory.getCoreService().isEnabled()) {
            return;
        }

        if (Agent.isDebugEnabled()) {
            getLogger().finer("Recording metrics for " + transactionData);
        }

        boolean sizeLimitExceeded = transactionData.getAgentAttributes().get(AttributeNames.SIZE_LIMIT_PARAMETER_NAME) != null;
        transactionStats.getUnscopedStats().getStats(MetricNames.SUPPORTABILITY_TRANSACTION_SIZE).recordDataPoint(transactionData.getTransactionSize());
        if (sizeLimitExceeded) {
            transactionStats.getUnscopedStats().getStats(MetricNames.SUPPORTABILITY_TRANSACTION_SIZE_CLAMP).incrementCallCount();
        }
        if (transactionData.getDispatcher() != null) {
            // notify listeners that txn is finished
            // TODO make a new PriorityTransactionListener that ensure they get notified first, use for TransactionTraceService
            for (PriorityTransactionListener listener : priorityTransactionListeners) {
                listener.dispatcherTransactionFinished(transactionData, transactionStats);
            }
            for (TransactionListener listener : transactionListeners) {
                listener.dispatcherTransactionFinished(transactionData, transactionStats);
            }
            for (ExtendedTransactionListener listener : extendedTransactionListeners) {
                listener.dispatcherTransactionFinished(transactionData, transactionStats);
            }
        } else {
            if (Agent.isDebugEnabled()) {
                getLogger().finer("Skipping transaction trace for " + transactionData);
            }
        }
        StatsService statsService = ServiceFactory.getStatsService();
        StatsWork statsWork = new MergeStatsEngineResolvingScope(transactionData.getBlameMetricName(), transactionData.getApplicationName(), transactionStats);
        statsService.doStatsWork(statsWork, transactionData.getBlameMetricName());
        if (transactionData.getDispatcher() != null) {
            for (TransactionStatsListener listener : transactionStatsListeners) {
                listener.dispatcherTransactionStatsFinished(transactionData, transactionStats);
            }
        }
    }

    @Override
    protected void doStart() {
        getLogger().finer("Transaction service starting");
        ServiceFactory.getHarvestService().addHarvestListener(new HarvestListener() {
            private volatile long txStarted = 0;
            private volatile long txFinished = 0;
            private volatile long txCancelled = 0;

            @Override
            public void beforeHarvest(String appName, StatsEngine statsEngine) {
            }

            @Override
            public void afterHarvest(String appName) {
                // We don't want to lock around this so the counters may be off by a few
                // counts relatively to each other each harvest. But the total counts should
                // be correct over the life of the service (typically, lifetime of the JVM).
                long started = txStartedThisHarvest.getAndSet(0);
                long finished = txFinishedThisHarvest.getAndSet(0);
                long cancelled = txCancelledThisHarvest.getAndSet(0);

                txStarted += started;
                txFinished += finished;
                txCancelled += cancelled;
                // The size() call is quite misleading because it may report entries that are
                // actually "dead" but have not yet been collected (see docs, they say this).
                recordTransactionSupportabilityMetrics(started, finished, cancelled);
                Agent.LOG.log(Level.FINE, "TransactionService: harvest: s/f/c {0}/{1}/{2}, total {3}/{4}/{5}, queue {6}",
                        started, finished, cancelled, txStarted, txFinished, txCancelled, updateQueue.size());
            }
        });
    }

    private void recordTransactionSupportabilityMetrics(long started, long finished, long cancelled) {
        StatsService statsService = ServiceFactory.getStatsService();

        //These three are so we can average the number of transactions happening per harvest cycle
        statsService.doStatsWork(StatsWorks.getRecordMetricWork(MetricNames.SUPPORTABILITY_HARVEST_TRANSACTION_STARTED, started),
                MetricNames.SUPPORTABILITY_HARVEST_TRANSACTION_STARTED);
        statsService.doStatsWork(StatsWorks.getRecordMetricWork(MetricNames.SUPPORTABILITY_HARVEST_TRANSACTION_FINISHED, finished),
                MetricNames.SUPPORTABILITY_HARVEST_TRANSACTION_FINISHED);
        statsService.doStatsWork(StatsWorks.getRecordMetricWork(MetricNames.SUPPORTABILITY_HARVEST_TRANSACTION_CANCELLED, cancelled),
                MetricNames.SUPPORTABILITY_HARVEST_TRANSACTION_CANCELLED);

        //These three are so we can get the total number of transactions ever, or the average among all apps' lifetime
        statsService.doStatsWork(StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_TRANSACTION_STARTED, (int) started),
                MetricNames.SUPPORTABILITY_HARVEST_TRANSACTION_STARTED);
        statsService.doStatsWork(StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_TRANSACTION_FINISHED, (int) finished),
                MetricNames.SUPPORTABILITY_TRANSACTION_FINISHED
        );
        statsService.doStatsWork(StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_TRANSACTION_CANCELLED, (int) cancelled),
                MetricNames.SUPPORTABILITY_HARVEST_TRANSACTION_CANCELLED);
    }

    @Override
    protected void doStop() {
        getLogger().finer("Transaction service stopping");
        priorityTransactionListeners.clear();
        transactionListeners.clear();
        extendedTransactionListeners.clear();
        transactionStatsListeners.clear();
        updateQueue.clear();
        shutdownQueue();
    }

    private void shutdownQueue() {
        getLogger().finer("Attempting graceful shutdown of transaction service");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                getLogger().finer("Graceful shutdown timed out, attempting forceful shutdown of transaction service");
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(15, TimeUnit.SECONDS)) {
                    getLogger().finer("Forceful shutdown timed out");
                }
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void addTransactionListener(PriorityTransactionListener listener) {
        priorityTransactionListeners.add(listener);
    }

    public void removeTransactionListener(PriorityTransactionListener listener) {
        priorityTransactionListeners.remove(listener);
    }

    public void addTransactionListener(TransactionListener listener) {
        transactionListeners.add(listener);
    }

    public void removeTransactionListener(TransactionListener listener) {
        transactionListeners.remove(listener);
    }

    public void addTransactionListener(ExtendedTransactionListener listener) {
        extendedTransactionListeners.add(listener);
    }

    public void removeTransactionListener(ExtendedTransactionListener listener) {
        extendedTransactionListeners.remove(listener);
    }

    public void addTransactionStatsListener(TransactionStatsListener listener) {
        transactionStatsListeners.add(listener);
    }

    public void removeTransactionStatsListener(TransactionStatsListener listener) {
        transactionStatsListeners.remove(listener);
    }

    public int getTransactionsInProgress() {
        return updateQueue.size();
    }

    public int getExpiredTransactionCount() {
        int expiredTransactions = 0;
        for (Transaction transaction : updateQueue.keySet()) {
            if (transaction.getTimeoutCause() != null) {
                expiredTransactions++;
            }
        }
        return expiredTransactions;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Transaction getTransaction(boolean createIfNotExists) {
        return Transaction.getTransaction(createIfNotExists);
    }

}
