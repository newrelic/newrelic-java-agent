/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;

import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

public class TokenImpl implements Token {

    private volatile Tracer initiatingTracer;
    private final AtomicBoolean active;

    public TokenImpl(Tracer tracer) {
        initiatingTracer = tracer;
        active = new AtomicBoolean(Boolean.TRUE);

        WeakRefTransaction weakRefTransaction = getTransaction();
        Transaction tx = weakRefTransaction == null ? null : weakRefTransaction.getTransactionIfExists();
        if (tx != null) {
            tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_CREATE);
        }

        if (DebugFlag.tokenEnabled.get()) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StackTraceElement location = stackTrace[4];
            if (location.getMethodName().equals("registerAsyncActivity")) {
                location = stackTrace[5];
            }

            Agent.LOG.log(Level.INFO, "Token: {0} created for Transaction: {1}, at: {2}", this, tx, location.toString());
        }
    }

    public WeakRefTransaction getTransaction() {
        // initiatingTracer could get null'ed out from underneath us so we need to grab the reference ourselves
        Tracer initiatingTracerCopy = initiatingTracer;

        if (initiatingTracerCopy != null && initiatingTracerCopy.getTransactionActivity() != null) {
            return new WeakRefTransaction(initiatingTracerCopy.getTransactionActivity().getTransaction());
        }
        return new WeakRefTransaction(null);
    }

    public Tracer getInitiatingTracer() {
        return initiatingTracer;
    }

    @Override
    public boolean expire() {
        if (active.compareAndSet(Boolean.TRUE, Boolean.FALSE)) {
            Transaction tx = getTransaction().getTransactionIfExists();
            if (tx != null) {
                tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_EXPIRE);
                MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_TOKEN_EXPIRE);
            }
            boolean expired = Transaction.expireToken(this);

            if (DebugFlag.tokenEnabled.get()) {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                StackTraceElement location = stackTrace[2];
                if (location.getMethodName().equals("registerAsyncActivity") || location.getMethodName().equals(
                        "startAsyncActivity") || location.getMethodName().equals("ignoreIfUnstartedAsyncContext")) {
                    location = stackTrace[4];
                } else if (location.getMethodName().equals("linkAndExpire")) {
                    location = stackTrace[3];
                }

                Agent.LOG.log(Level.INFO, "Token: {0} expired for Transaction: {1}, at: {2}", this, tx, location.toString());
            }

            return expired;
        } else {
            Agent.LOG.log(Level.FINER, "Token has already been expired {0}.", this);
        }
        return false;
    }

    @Override
    public boolean link() {
        boolean linked = Transaction.linkTxOnThread(this);
        WeakRefTransaction weakRefTransaction = getTransaction();
        Transaction tx = weakRefTransaction == null ? null : weakRefTransaction.getTransactionIfExists();

        String locationString = null;
        if (DebugFlag.tokenEnabled.get()) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StackTraceElement location = stackTrace[2];
            if (location.getMethodName().equals("startAsyncActivity")) {
                location = stackTrace[4];
            } else if (location.getMethodName().equals("linkAndExpire")) {
                location = stackTrace[3];
            }
            locationString = location.toString();
        }

        if (tx != null) {
            if (DebugFlag.tokenEnabled.get()) {
                if (!linked && TransactionActivity.get() == null) {
                    Agent.LOG.log(Level.WARNING, "Token: {0} was NOT linked because there was no Transaction in "
                            + "progress. Did you forget to add @Trace(async = true) to: {1}?", this, locationString);
                } else {
                    Agent.LOG.log(Level.INFO, "Token: {0} {1} for Transaction: {2}, at: {3}", this, linked ? "linked"
                            : "link ignored", tx, locationString);
                }
            }

            if (linked) {
                tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_LINK_SUCCESS);
            } else {
                tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_LINK_IGNORE);
            }
        }

        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_TOKEN_LINK);

        return linked;
    }

    @Override
    public boolean linkAndExpire() {
        boolean linked = link();
        boolean expired = expire();
        return linked && expired;
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    /**
     * This is used by the transaction to expire tokens when removed from its token cache.
     */
    void markExpired() {
        active.set(Boolean.FALSE);
        Transaction tx = getTransaction().getTransactionIfExists();
        if (tx != null) {
            tx.onRemoval();
        }
        initiatingTracer = null;
    }

    void setTruncated() {
        final Tracer tracer = initiatingTracer;
        if (tracer != null) {
            tracer.setMetricNameFormatInfo(tracer.getMetricName(), "Truncated/" + tracer.getMetricName(), tracer.getTransactionSegmentUri());
            String timeoutCauseMetric = MessageFormat.format(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT_CAUSE, tracer.getClassMethodSignature());
            ServiceFactory.getStatsService().doStatsWork(StatsWorks.getIncrementCounterWork(timeoutCauseMetric, 1), timeoutCauseMetric );
        } else {
            Agent.LOG.log(Level.FINEST, "Initiating tracer is null. Unable to mark segment as truncated.");
        }
    }

    public Runnable wrap(Runnable runnable, final String metricName) {
        return () -> {
            com.newrelic.agent.bridge.Transaction transaction = AgentBridge.getAgent().getTransaction(true);
            Segment segment = transaction.startSegment(metricName);
            try {
                link();
                NewRelic.getAgent().getTracedMethod().setMetricName(metricName);
                runnable.run();
            } finally {
                segment.end();
                expire();
            }
        };
    }


    public <T> Callable<T> wrap(Callable<T> callable, String metricName) {
        return () -> {
            try {
                link();
                NewRelic.getAgent().getTracedMethod().setMetricName(metricName);
                return callable.call();
            } finally {
                expire();
            }
        };
    }

    public <T, U> Function<T, U> wrapFunction(Function<T, U> function, String metricName) {
        return t -> {
            try {
                link();
                NewRelic.getAgent().getTracedMethod().setMetricName(metricName);
                return function.apply(t);
            } finally {
                expire();
            }
        };
    }

    public <T, U, V> BiFunction<T, U, V> wrapFunction(BiFunction<T, U, V> function, String metricName) {
        return (t, u) -> {
            try {
                link();
                NewRelic.getAgent().getTracedMethod().setMetricName(metricName);
                return function.apply(t, u);
            } finally {
                expire();
            }
        };
    }

    public <T> Consumer<T> wrapConsumer(Consumer<T> consumer, String metricName) {
        return t -> {
            try {
                link();
                NewRelic.getAgent().getTracedMethod().setMetricName(metricName);
                consumer.accept(t);
            } finally {
                expire();
            }
        };
    }

    public <T, U> BiConsumer<T, U> wrapConsumer(BiConsumer<T, U> consumer, String metricName) {
        return (t, u) -> {
            try {
                link();
                NewRelic.getAgent().getTracedMethod().setMetricName(metricName);
                consumer.accept(t, u);
            } finally {
                expire();
            }
        };
    }

    public <T> Supplier<T> wrapSupplier(Supplier<T> supplier, String metricName) {
        return () -> {
            linkAndExpire();
            NewRelic.getAgent().getTracedMethod().setMetricName(metricName);
            return supplier.get();
        };
    }
}
