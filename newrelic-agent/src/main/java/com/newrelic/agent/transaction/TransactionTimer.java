/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.tracers.Tracer;

/*
 * Historically the agent has reported a response time which is typically when the method wrapping the handling of the request and response finishes.
 * This is generally (but not always) associated with time to last byte. We want to move a web transaction to time to first byte and time to last byte. 
 * This means we now have three web request counters. Time to first byte, time to last byte, and the legacy response time. 
 * 
 * Time to first byte and time to last byte should only be set for web transactions.
 * Response time needs to be set for web transactions and background transactions. It is used as the general "duration" for a transaction where
 * "duration" is response time for web transaction or the wall clock time for background transactions.
 */
public class TransactionTimer {

    // start time for the transaction
    private final long startTimeNs;
    // sum of all root tracers (or sum of exclude time of all tracers)
    private volatile long totalTimeNs;
    // duration from response in to last byte out
    // we generally approximate this to end of method handling the request/response
    private final AtomicLong timeToLastByteNs;

    // duration from response in to first byte out
    // we generally approximate this to last time
    private final AtomicLong timeToFirstByteNs;
    // the time the last tracer finished
    private volatile long transactionEndTimeNs;
    // this is what the agent reports as the response time
    private final AtomicLong responseTimeNs;

    // used to timeout the tokens in the transaction
    private volatile long timeLastTxaFinished;

    public TransactionTimer(long startTimeNs) {
        this.startTimeNs = startTimeNs;
        timeToLastByteNs = new AtomicLong(0);
        timeToFirstByteNs = new AtomicLong(0);
        responseTimeNs = new AtomicLong(0);
    }

    // this is updated on the fly for the transaction
    public void markTxaFinishTime(Tracer rootTracer) {
        timeLastTxaFinished = Math.max(timeLastTxaFinished, rootTracer.getEndTime());
    }

    public long getTimeLastTxaFinished() {
        return timeLastTxaFinished;
    }

    public boolean markTimeToLastByte(long endTimeNs) {
        long duration = Math.max(0, (endTimeNs - startTimeNs));
        return (duration > 0 && timeToLastByteNs.compareAndSet(0, duration));
    }

    /**
     * Set the response time of the transaction to the given timestamp.
     * Successive calls will have no effect (first wins).
     *
     * @param endTimeNs Nanosecond timestamp of the response time
     * @return true if the response time was set to endTimeNs
     */
    public boolean markResponseTime(long endTimeNs) {
        if (responseTimeNs.compareAndSet(0, Math.max(0, (endTimeNs - startTimeNs)))) {
            Agent.LOG.log(Level.FINEST, "tx response time set: {0}", responseTimeNs.get());
            return true;
        }
        // internal callers will often hit this case so we'll only log failures at the api level
        return false;
    }

    public boolean markTimeToFirstByte(long endTimeNs) {
        /*
         * If the last byte is already set, then do not set the first byte. It is impossible to send the first byte
         * after the last byte.
         */
        return (timeToLastByteNs.get() == 0)
                && timeToFirstByteNs.compareAndSet(0, Math.max(0, (endTimeNs - startTimeNs)));
    }

    // only the last thread should call this to set the response time correctly for background transactions
    public void markTransactionAsDone() {
        // if no response time has been set, use the end time
        markResponseTime(transactionEndTimeNs);
    }

    // only the last thread should call this
    public void markTransactionActivityAsDone(long newEndTimeNs, long durationNs) {
        if (newEndTimeNs > transactionEndTimeNs) {
            this.transactionEndTimeNs = newEndTimeNs;
        }
        totalTimeNs += (durationNs);
    }

    public long getTimeToFirstByteInNanos() {
        return timeToFirstByteNs.get();
    }

    public long getTimetoLastByteInNanos() {
        return timeToLastByteNs.get();
    }

    /*
     * This should be called after a transaction is finished and not during the transaction.
     */
    public long getResponseTimeInNanos() {
        return responseTimeNs.get();
    }

    /*
     * This should be called during the transaction to determine how much time has passed.
     */
    public long getRunningDurationInNanos() {
        return (responseTimeNs.get() > 0 ? responseTimeNs.get() : Math.max(0, System.nanoTime() - startTimeNs));
    }

    public long getTotalSumTimeInNanos() {
        return totalTimeNs;
    }

    public long getStartTimeInNanos() {
        return startTimeNs;
    }

    public long getEndTimeInNanos() {
        return transactionEndTimeNs;
    }

    public long getTransactionDurationInNanos() {
        return Math.max(0, (transactionEndTimeNs - startTimeNs));
    }

    public long getResponseTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getResponseTimeInNanos(), TimeUnit.NANOSECONDS);
    }

    public long getEndTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(transactionEndTimeNs, TimeUnit.NANOSECONDS);
    }

    /*
     * This should match the response time for all background transactions. It should be greater than or equal to the
     * response time for all web transactions.
     */
    public long getTransactionDurationInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(Math.max(0, (transactionEndTimeNs - startTimeNs)), TimeUnit.NANOSECONDS);
    }

}
