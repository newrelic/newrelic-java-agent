/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.MapMaker;
import com.newrelic.agent.bridge.AsyncApi;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.servlet.ServletAsyncTransactionStateImpl;
import com.newrelic.api.agent.Logger;
import org.objectweb.asm.Opcodes;

import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

/**
 * This is the legacy asynchronous API. All methods are used for legacy instrumentation packages only and should not be
 * used in new code.
 */
public class AsyncApiImpl implements AsyncApi {
    // does not permit null keys or values
    private final ConcurrentMap<Object, Transaction> asyncTransactions = new MapMaker().weakKeys().makeMap();
    private final Logger logger;
    private final ConfigService configService = ServiceFactory.getConfigService();

    public AsyncApiImpl(Logger logger) {
        this.logger = logger;
    }

    /**
     * Suspend the transaction.
     */
    @Override
    public void suspendAsync(Object asyncContext) {
        logger.log(Level.FINEST, "Suspend async");
        if (asyncContext != null) {
            Transaction currentTxn = Transaction.getTransaction(false);
            if (currentTxn != null) {
                Transaction preExistingTransaction = asyncTransactions.get(asyncContext);
                if (!configService.getDefaultAgentConfig().legacyAsyncApiSkipSuspend() || preExistingTransaction == null ||
                        currentTxn == preExistingTransaction) {
                    // Only one transaction can be suspended and resumed for a given AsyncContext.
                    // If no transaction is mapped to the current AsyncContext instance,
                    // then map the transaction and allow it to be suspended.
                    // If the current transaction is the same instance as the pre-existing transaction that
                    // is already mapped to the current AsyncContext instance, then allow it to be suspended again.
                    suspendTransaction(asyncContext, currentTxn);
                } else {
                    // Avoid suspending any additional transactions that map to the current AsyncContext instance,
                    // instead letting them complete naturally. This scenario should only occur in rare cases (e.g. Jetty
                    // ContextHandler.doHandle exhibited such behavior when combined with Karaf and Camel CXF).
                    // Failing to do so could lead to a memory leak as previously suspended transactions that were
                    // in the asyncTransactions map would be overwritten and never resumed/completed.
                    ServiceFactory.getStatsService().doStatsWork(
                            StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_SKIP_SUSPEND, 1),
                            MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_SKIP_SUSPEND);

                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST,
                                "Skipped suspending Transaction: {0} with AsyncContext: {1} because Transaction: {2} is already suspended for that AsyncContext.",
                                currentTxn, asyncContext, preExistingTransaction);
                    }
                }
            }
        }
    }

    /**
     * This suspends the current Transaction and maps it to the AsyncContext instance of the request,
     * which can later be used to resume the Transaction on whichever thread the async work executes on.
     *
     * @param asyncContext AsyncContext instance for the current request
     * @param currentTxn   Transaction instance to be suspended
     */
    private void suspendTransaction(Object asyncContext, Transaction currentTxn) {
        TransactionState transactionState = setTransactionState(currentTxn);
        transactionState.suspendRootTracer();
        asyncTransactions.put(asyncContext, currentTxn);
        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_SUSPEND, 1),
                MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_SUSPEND);
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Suspended async: {0}, for Transaction: {1}", asyncContext, currentTxn);
        }
    }

    /**
     * Set the transaction state to an instance of {@link ServletAsyncTransactionStateImpl}
     */
    private TransactionState setTransactionState(Transaction tx) {
        TransactionState txState = tx.getTransactionState();
        if (txState instanceof ServletAsyncTransactionStateImpl) {
            return txState;
        }
        txState = new ServletAsyncTransactionStateImpl(tx);
        tx.setTransactionState(txState);
        return txState;
    }

    /**
     * Resume the suspended transaction.
     */
    @Override
    public com.newrelic.agent.bridge.Transaction resumeAsync(Object asyncContext) {
        logger.log(Level.FINEST, "Resume async");
        if (asyncContext != null) {
            Transaction suspendedTx = asyncTransactions.get(asyncContext);
            ServiceFactory.getStatsService().doStatsWork(
                    StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_RESUME, 1),
                    MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_RESUME);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Resume async: {0}, for Transaction: {1}", asyncContext, suspendedTx);
            }
            if (suspendedTx != null) {
                suspendedTx.getTransactionState().resume();
                if (suspendedTx.isStarted()) {
                    suspendedTx.getTransactionState().getRootTracer();
                    return new BoundTransactionApiImpl(suspendedTx);
                }
            }
        }
        return TransactionApiImpl.INSTANCE;
    }

    /**
     * Complete the resumed transaction.
     */
    @Override
    public void completeAsync(Object asyncContext) {
        logger.log(Level.FINEST, "Complete async");
        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_COMPLETE, 1),
                MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_COMPLETE);
        if (asyncContext == null) {
            logger.log(Level.FINEST, "Complete async context is null");
            return;
        }
        Transaction transaction = asyncTransactions.remove(asyncContext);
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Complete async: {0}, for Transaction: {1}", asyncContext, transaction);
        }
        if (transaction != null) {
            transaction.getTransactionState().complete();
        }
    }

    /**
     * Record error condition with the resumed transaction.
     */
    @Override
    public void errorAsync(Object asyncContext, Throwable t) {
        logger.log(Level.FINEST, "Error async");
        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_ERROR, 1), MetricNames.SUPPORTABILITY_ASYNC_API_LEGACY_ERROR);
        if (asyncContext == null || t == null) {
            logger.log(Level.FINEST, "Error async context or throwable is null");
            return;
        }
        Transaction transaction = asyncTransactions.get(asyncContext);
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Error async: {0}, for Transaction: {1}", asyncContext, transaction);
        }
        if (transaction != null) {
            transaction.setThrowable(t, TransactionErrorPriority.API, false);
        }
    }

    @Override
    public void finishRootTracer() {
        Transaction currentTxn = Transaction.getTransaction(false);
        if (currentTxn != null) {
            Tracer rootTracer = currentTxn.getRootTracer();
            if (rootTracer != null) {
                rootTracer.finish(Opcodes.RETURN, null);
            }
        }
    }
}
