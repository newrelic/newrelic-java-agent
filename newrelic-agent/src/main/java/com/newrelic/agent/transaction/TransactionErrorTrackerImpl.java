/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.util.StackTraces;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class TransactionErrorTrackerImpl implements TransactionErrorTracker {
    private static final String SERVLET_EXCEPTION_CLASS_NAME = "javax.servlet.ServletException";
    private volatile TransactionThrowable throwable; // guarded by throwablePriority
    private final ConcurrentMap<Integer, String> tracerErrors = new ConcurrentHashMap<>();
    private final AtomicReference<TransactionErrorPriority> throwablePriority = new AtomicReference<>();

    /**
     * If the exception is a <tt>javax.servlet.ServletException</tt> and the cause of the exception is not null, return
     * the cause. Otherwise, return the exception.
     *
     * Since most uncaught exceptions are <tt>javax.servlet.ServletException</tt> it's more informative to report the
     * root cause to RPM.
     */
    public static Throwable unwrapIfServletException(Throwable throwable) {
        if (throwable != null) {
            if (SERVLET_EXCEPTION_CLASS_NAME.equals(throwable.getClass().getName())) {
                return StackTraces.getRootCause(throwable);
            }
        }
        return throwable;
    }

    @Override
    public TransactionThrowable getThrowable() {
        return throwable;
    }

    @Override
    public void setThrowable(TransactionThrowable transactionThrowable) {
        throwable = transactionThrowable;
    }

    @Override
    public void setThrowable(Throwable throwable, TransactionErrorPriority priority, boolean expected, String mostRecentSpanId) {
        if (throwable == null) {
            return;
        }

        Agent.LOG.log(Level.FINER, "Set throwable {0} (expected {1}) in transaction {2}", throwable.getClass().getName(),
                expected, this);

        Throwable rootCause = unwrapIfServletException(throwable);

        final String spanId = getSpanIdForThrowable(priority, throwable, mostRecentSpanId);

        setThrowable(new TransactionThrowable(rootCause, expected, spanId));
    }

    @Override
    public boolean tryUpdatePriority(TransactionErrorPriority newPriority) {
        return newPriority.updateCurrentPriority(throwablePriority);
    }

    @Override
    public TransactionErrorPriority getPriority() {
        return throwablePriority.get();
    }

    /**
     * Capture the first tracer that had this unhandled exception.
     *
     * @param throwable The unhandled exception from the tracer
     * @param spanId The Tracer GUID
     */
    @Override
    public void noticeTracerException(Throwable throwable, String spanId) {
        if (throwable == null) {
            return;
        }
        int hashCode = System.identityHashCode(throwable);
        tracerErrors.putIfAbsent(hashCode, spanId);
    }

    /**
     * Identifies the most-likely span for this throwable. If the throwable was set through the API,
     * then we want to use the current span (as that's what we were told by the user). If the throwable
     * was simply unhandled and escaped the transaction, we want to get the first span where it was
     * seen.
     * @param priority The priority, which identifies if the throwable is set via API or Tracer.
     * @param throwable The exception we're setting.
     * @param mostRecentSpanId The GUID of the last tracer in the activity
     * @return The applicable spanId
     */
    private String getSpanIdForThrowable(TransactionErrorPriority priority, Throwable throwable, String mostRecentSpanId) {
        return priority == TransactionErrorPriority.API
                ? getCurrentSpanThenFallBackToNoticed(throwable, mostRecentSpanId)
                : getNoticedSpanThenFallBackToCurrent(throwable, mostRecentSpanId);
    }

    private String getNoticedSpanThenFallBackToCurrent(Throwable throwable, String mostRecentSpanId) {
        String noticedSpan = safeGetSpanIdForThrowable(throwable);
        return noticedSpan != null ? noticedSpan : mostRecentSpanId;
    }

    private String getCurrentSpanThenFallBackToNoticed(Throwable throwable, String mostRecentSpan) {
        return mostRecentSpan != null ? mostRecentSpan : safeGetSpanIdForThrowable(throwable);
    }

    /**
     * Tries to identify the span ID that corresponds to this error. If it was not previously seen,
     * use the last tracer as of when this method was called.
     *
     * @param throwable The error we're trying to identify
     * @return The span in which this error was first identified.
     */
    private String safeGetSpanIdForThrowable(Throwable throwable) {
        return tracerErrors.get(System.identityHashCode(throwable));
    }
}
