/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.NoOpTracer;
import com.newrelic.agent.tracers.SkipTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.transaction.TransactionCache;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * This class tracks the state of a single asynchronous activity within a transaction. An instance of this class may be
 * associated with e.g. a thread, the execution of a task on pool thread, or the execution of a "coroutine" on a thread.<br>
 * <br>
 * This class is not threadsafe, but is typically referenced by two distinct threads during its lifetime. First, it
 * holds state for its associated thread (or task, etc.) until its root tracer completes. It may then be queued
 * (indirectly through its owning transaction) for harvest, during which it is referenced by the harvest thread. These
 * two usages never overlap in time. Memory visibility is assured because the transaction will pass through a
 * synchronized object (e.g. atomic or concurrent collection) that has the effect of inserting a full barrier between
 * the async activity's last write and the harvest thread's first read.
 */
public class TransactionActivity {
    public static final int NOT_REPORTED = -1;
    private volatile List<Tracer> tracers;

    /*
     * This object has a complex life cycle. It is always created by a Transaction as the Transaction comes into
     * existence as the result of a call from instrumentation (whether pointcut or weaved). Code in this object
     * (tracerStarted()) may then discover that this asynchronous activity should not be part of a Transaction at this
     * time, e.g. because the root tracer has not been set and the "encountered" tracer is not an initiator tracer. In
     * this case it may clear the Transaction and TransactionActivity from thread local storage. Once an initiator
     * tracer is encountered and the root tracer is set, this object will exist in thread local storage until the root
     * tracer completes. After that, it will continue to exist until its owning Transaction is GC'd, but will not
     * accessible from any thread's local storage.
     */

    private Tracer rootTracer;
    private Tracer lastTracer;

    private final TransactionStats transactionStats;

    private Transaction transaction;
    private final TransactionCache transactionCache;
    private final long threadId;
    private Segment segment;

    /**
     * If true, this Txa is not stored in a thread local.
     */
    private final boolean notInThreadLocal;

    private final long cpuStartTimeInNanos;
    private long totalCpuTimeInNanos;

    private boolean sendsResponse = false;

    private volatile String asyncContext;

    private int tracerStartLock;

    // This value can be set per-activity by an API call. In addition, we cache the fact that the owning transaction
    // has been ignored here in the activity in order to reduce the cost of creating tracers, since it's checked
    // every time. Must be volatile because calls from the Transaction object can occur on any thread.
    private volatile boolean activityIsIgnored = false;

    /**
     * This is used to denote the fact that a flyweight method call is in progress
     */
    private boolean flyweightInProgress;

    /*
     * activityId is used for overriding hashcode because Object.hashCode() had rather high overhead. When migrating an
     * activity to a new transaction, the activityId should be updated to reduce collisions. We must be very careful to
     * only modify it when the object does not exist in any hashmaps, otherwise we may be unable to find it again.
     * Changing the hash code of an object can make it impossible to find the object in a collection. We change this
     * value only a single point in the Activity's life cycle when it is known not to be in any collections. This
     * unusual design was the result of careful performance work.
     */
    private int activityId;

    // Used to determine if the work for this transaction activity has completed.
    private volatile boolean isDone = false;

    private static final ThreadLocal<TransactionActivity> activityHolder = new ThreadLocal<TransactionActivity>() {
        @Override
        public TransactionActivity get() {
            return super.get();
        }

        @Override
        public void set(TransactionActivity value) {
            super.set(value);
        }

        @Override
        public void remove() {
            super.remove();
        }
    };

    /**
     * Clear the TransactionActivity from the thread local that holds it. This is a "dangerous" interface that is
     * required for instrumentation in which the transaction+activity are multiplexed on a thread (e.g. "coroutine" or
     * "continuation" mechanisms such as Javaflow or the async servlet interface).
     */
    public static void clear() {
        activityHolder.remove();
        Agent.LOG.log(Level.FINEST, "TransactionActivity.clear()");
    }

    /**
     * Jam the argument into the thread local that holds it. This is a "dangerous" interface that is required for
     * instrumentation in which the transaction+activity are multiplexed on a thread (e.g. "coroutine" or "continuation"
     * mechanisms such as Javaflow or the async servlet interface).
     *
     * @param txa the new value to make current
     */
    public static void set(TransactionActivity txa) {
        activityHolder.set(txa);
        Agent.LOG.log(Level.FINEST, "TransactionActivity.set({0})", txa);
    }

    public static TransactionActivity get() {
        return activityHolder.get();
    }

    public static TransactionActivity create(Transaction transaction, int id) {
        TransactionActivity txa = new TransactionActivity(transaction, Thread.currentThread().getId(),
                Thread.currentThread().getName(), false);
        txa.activityId = id;
        activityHolder.set(txa);
        Agent.LOG.log(Level.FINE, "created {0} for {1}", txa, transaction);
        return txa;
    }

    /**
     * Creates a TransactionActivity. Does not set the activity in the activityHolder; so it will not override whatever
     * activity is currently in progress.
     *
     * @param transaction parent transaction
     * @param id ID of the activity
     * @param asyncContext name of async context; since external async spans multiple threads, this is typically the
     * thread name that this is running on.
     * @return activity
     */
    public static TransactionActivity createWithoutHolder(Transaction transaction, int id, String asyncContext) {
        TransactionActivity txa = new TransactionActivity(transaction, NOT_REPORTED, asyncContext, true);
        txa.activityId = id;
        Agent.LOG.log(Level.FINE, "created {0} for {1}", txa, transaction);
        return txa;
    }

    /**
     * Return true if the tracer being created is allowed to create a new segment.
     *
     * @return true if the tracer being created is allowed to create a new segment.
     */
    public boolean canCreateTransactionSegment() {
        if (transaction == null) {
            // We may be creating the root tracer on the fast path,
            // and don't have a transaction yet. We always allow
            // the tracer to proceed in this case.
            return ServiceFactory.getTransactionTraceService().isEnabled();
        }
        return transaction.shouldGenerateTransactionSegment();
    }

    private TransactionActivity(Transaction tx, long threadId, String asyncContext, boolean notInThreadLocal) {
        this.transaction = tx;
        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        tracers = null;
        transactionStats = new TransactionStats();
        transactionCache = new TransactionCache();
        this.threadId = threadId;
        this.asyncContext = asyncContext;
        this.notInThreadLocal = notInThreadLocal;

        if (ttService.isEnabled()) {
            if (ttService.isThreadCpuTimeEnabled()) {
                ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                cpuStartTimeInNanos = threadMXBean.getCurrentThreadCpuTime();
                totalCpuTimeInNanos = 0;
            } else {
                cpuStartTimeInNanos = NOT_REPORTED;
                totalCpuTimeInNanos = NOT_REPORTED;
            }
        } else {
            cpuStartTimeInNanos = NOT_REPORTED;
            totalCpuTimeInNanos = NOT_REPORTED;
        }
    }

    /**
     * The public constructor may be used only for test purposes.
     */
    public TransactionActivity() {
        String realClassName = getClass().getSimpleName();
        if (!realClassName.startsWith("Mock")) {
            throw new IllegalStateException("the public constructor is only for test purposes.");
        }
        tracers = null;
        transactionStats = null;
        transactionCache = null;
        notInThreadLocal = false;
        threadId = NOT_REPORTED;
        asyncContext = "MockThread";
        cpuStartTimeInNanos = NOT_REPORTED;
        totalCpuTimeInNanos = NOT_REPORTED;
    }

    public void markAsResponseSender() {
        Agent.LOG.log(Level.FINEST, "Transaction Activity {0} marked as the response sender", this);
        sendsResponse = true;
    }

    public long getThreadId() {
        return threadId;
    }

    /**
     * @return the async context associated with this transaction activity. This shows up on transaction traces.
     */
    public String getAsyncContext() {
        return asyncContext;
    }

    /**
     * set the async context associated with this transaction activity. This shows up on transaction traces.
     */
    public void setAsyncContext(String asyncContext) {
        this.asyncContext = asyncContext;
    }

    public TransactionStats getTransactionStats() {
        return transactionStats;
    }

    /**
     * Return the current state of the tracer stack for this activity.
     *
     * @return the current state of the tracer stack for this activity.
     */
    public List<Tracer> getTracers() {
        return Collections.unmodifiableList(tracers == null ? Collections.<Tracer>emptyList() : tracers);
    }

    /**
     * Returns the total cpu time in nanoseconds;
     *
     */
    public long getTotalCpuTime() {
        return totalCpuTimeInNanos;
    }

    public boolean isNotInThreadLocal() {
        return notInThreadLocal;
    }

    public void setTotalCpuTime(long totalCpuTimeInNanos) {
        this.totalCpuTimeInNanos = totalCpuTimeInNanos;
    }

    /*-
     * Implementation of API call to ignore an async activity without ignoring the entire transaction.
     * Probably should have been named "...cancel" instead of ignore. Ignored activities can never
     * transition back to being un-ignored ("regarded"?) activities.
     */
    public void setToIgnore() {
        activityIsIgnored = true;
    }

    public boolean isIgnored() {
        return activityIsIgnored;
    }

    /**
     * Cache the fact that the owning transaction has been ignored. This reduces the cost of making this check, which is
     * performed in the "hot path" of Tracer creation.
     *
     * @param newState the new state. Transitions from false to true when e.g. ignore is called on the API. Transitions
     * back to false if an activity is reparented from a previous-ignored transaction to a non-ignored
     * transaction.
     */
    void setOwningTransactionIsIgnored(boolean newState) {
        activityIsIgnored = newState;
    }

    /**
     * Adds a tracer to the call stack.
     *
     * @param tracer
     */
    public Tracer tracerStarted(Tracer tracer) {
        Tracer tr = addTracerToStack(tracer);
        if (tr != null && getTransaction() != null && tr.isTransactionSegment()) {
            getTransaction().getTransactionCounts().addTracer();
        }
        return tr;
    }

    /**
     * Copies a tracer to the call stack.
     *
     * @param tracer
     * @return The tracer if it was successfully added to the stack or null if it wasn't
     */
    public Tracer addTracerToStack(Tracer tracer) {
        if (isTracerStartLocked()) {
            Agent.LOG.log(Level.FINER, "tracerStarted ignored: tracerStartLock is already active");
            return null;
        }

        if (!isStarted()) {
            // Must be an initiator type of tracer to start things
            if (tracer instanceof TransactionActivityInitiator) {
                setRootTracer(tracer);
            } else {
                return null;
            }
        } else {
            if (tracer.getParentTracer() != null) {
                lastTracer = tracer;
                if (Agent.isDebugEnabled() && Agent.LOG.isFinestEnabled()) {
                    Agent.LOG.log(Level.FINEST, "Tracer Debug: called addTracerToStack, lastTracer (pointer to top of stack) set to {0}", tracer);
                }
                addTracer(tracer);
            } else {
                if (Agent.LOG.isFinestEnabled()) {
                    Agent.LOG.log(Level.FINEST, "tracerStarted: {0} cannot be added: no parent pointer", tracer);
                }
                return null;
            }
        }

        return tracer;
    }

    /**
     * Pop the finished tracer off the call stack.
     */
    public void tracerFinished(Tracer tracer, int opcode) {
        if (tracer instanceof SkipTracer) {
            if (Agent.isDebugEnabled() && Agent.LOG.isFinestEnabled()) {
                Agent.LOG.log(Level.FINEST, "Tracer Debug: called tracerFinished to pop tracer off stack, ignoring SkipTracer tracer = {0}", tracer);
            }
            return;
        }
        if (tracer != lastTracer) {
            failedDueToInconsistentTracerState(tracer, opcode);
        } else if (tracer == rootTracer) {
            finished(rootTracer, opcode);
        } else {
            lastTracer = tracer.getParentTracer();
            if (Agent.isDebugEnabled() && Agent.LOG.isFinestEnabled()) {
                Agent.LOG.log(Level.FINEST, "Tracer Debug: called tracerFinished to pop tracer off stack, lastTracer (pointer to top of stack) set to {0}, tracer (actual tracer popped off stack) = {1}", lastTracer, tracer);
            }
        }
    }

    /**
     * A serious internal error occurred. All data associated with this activity will be lost.
     *
     * Note: We started seeing this error while investigating absurd metric values
     * (e.g. - negative durations, or call counts in the hundreds of millions)
     * The fix was to remove the ProcessPointCut and replace it with
     * a weaver instrumentation module.
     * We've left this code in place just in case we see the problem in other places.
     *
     * The offending code that produced this error looked something like this:
     * It may be worth noting that myServiceMethod() was being called by another service method
     * that was also annotated with @Trace(dispatcher = true) and that method was also
     * called the same way.  And that 3rd method was being called every 1 second
     * by a ScheduledThreadPoolExecutor.  And the mapOfStringToListOfStrings should have at least 2 entries.
     *
     *  @Trace(dispatcher = true, metricName = "myMetricName")
     *  public void myServiceMethod () {
     *      ...
     *      CompletableFuture.allOf(
     *          mapOfStringToListOfStrings.entrySet().stream().map(entry -> CompletableFuture.runAsync(() -> {
     *              ...
     *              callSomeMethodThatUsesProcessWaitFor();
     *              ...
 *              })).toArray(CompletableFuture[]::new)
     *      ).join();
     *      ...
     *  }
     *
     * @param tracer the tracer that completed, leading to the internal error detection.
     * @param opcode
     */
    private void failedDueToInconsistentTracerState(Tracer tracer, int opcode) {
        Agent.LOG.log(Level.SEVERE, "Tracer Debug: Inconsistent state! tracer (actual tracer popped off stack) != lastTracer (pointer to top of stack) for {0} ({1} != {2})", this, tracer,
                lastTracer);
        try {
            transaction.activityFailedOrIgnored(this, opcode);
        } finally {
            if (!isNotInThreadLocal()) {
                activityHolder.remove();
            }
        }
    }

    /**
     * Called when the root tracer is successfully popped off the call stack.
     *
     * @param tracer
     * @param opcode
     */
    private void finished(Tracer tracer, int opcode) {
        if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.log(Level.FINEST, "tracerFinished: {0} opcode: {1} in transactionActivity {2}", tracer, opcode, this);
        }
        try {
            if (transaction != null) {
                if (!activityIsIgnored) {
                    recordCpu();
                    if (sendsResponse) {
                        Agent.LOG.log(Level.FINER, "Txa {0} marked as response sender", this);
                        // if no caller has marked the response we'll set it to the end of the response sender txa
                        getTransaction().getTransactionTimer().markResponseTime(rootTracer.getEndTime());
                        if (getTransaction().getDispatcher() != null) {
                            // Parse dispatcher request headers before we get rid of request/response objects
                            getTransaction().getInboundHeaderState();
                            getTransaction().getDispatcher().transactionActivityWithResponseFinished();
                        }
                    }
                    transaction.activityFinished(this, tracer, opcode);
                } else {
                    transaction.activityFailedOrIgnored(this, opcode);
                }
            }
            isDone = true;
        } finally {
            if (!isNotInThreadLocal()) {
                activityHolder.remove();
            }
        }
    }

    public boolean isStarted() {
        return rootTracer != null;
    }

    public boolean isFinished() {
        return isDone;
    }

    /**
     * Return true if the tracer at the top of tracer stack is a flyweight tracer (leaf by default) or a leaf tracer.
     *
     * @return true if the tracer at the top of tracer stack is a flyweight tracer (leaf by default) or a leaf tracer.
     */
    public boolean isLeaf() {
        return flyweightInProgress || (lastTracer != null && lastTracer.isLeaf());
    }

    public void recordCpu() {
        if (cpuStartTimeInNanos != NOT_REPORTED && totalCpuTimeInNanos == 0) {
            totalCpuTimeInNanos = ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime()
                    - cpuStartTimeInNanos;
        }
    }

    public void addTracer(Tracer tracer) {
        if (tracer.isTransactionSegment()) {
            if (getTransaction() != null) { // txn may be null if @Trace(async = true)
                // this means if you have more than one tracer (the root tracer) and you do not have a
                // transaction yet (async = true), then you will only get metrics and you will not
                // be in the transaction trace
                if (tracers == null) {
                    tracers = new ArrayList<>();
                }
                tracers.add(tracer);
            }
        }
    }

    private void setRootTracer(Tracer tracer) {
        rootTracer = tracer;
        lastTracer = tracer;
        if (Agent.isDebugEnabled() && Agent.LOG.isFinestEnabled()) {
            Agent.LOG.log(Level.FINEST, "Tracer Debug: called setRootTracer, lastTracer (pointer to top of stack) and rootTracer set to {0}", tracer);
        }

        if (tracer instanceof DefaultTracer) {
            DefaultTracer dt = (DefaultTracer) tracer;
            // Only check limits if in a transaction
            dt.setAttribute("async_context", asyncContext, !tracer.isAsync(), false, false);
        }

        if (!tracer.isAsync() && transaction != null) {
            transaction.activityStarted(this);
        }
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public Segment getSegment() {
        return this.segment;
    }

    /**
     * The tracer start lock is designed to prevent tracers from being created during the creation of another tracer.
     * Acquire the lock. Note: fast path callers should use checkTracerStart() to reduce the number of method calls to
     * this object.
     */
    /*-
       Implementation note: In version 3.17.0 of the Agent, and after much thought and discussion, we removed the tracer
       start lock as an optimization because we couldn't identify the scenario in which it (still?) mattered. Unfortunately,
       a customer quickly encountered this:

         ...
         at com.newrelic.agent.CrossProcessTransactionStateImpl.processInboundResponseHeaders(CrossProcessTransactionStateImpl.java:275)
         at com.newrelic.agent.tracers.AbstractCrossProcessTracer.doFinish(AbstractCrossProcessTracer.java:26)
         at com.newrelic.agent.tracers.DefaultTracer.finish(DefaultTracer.java:172)
         at com.newrelic.agent.tracers.AbstractTracer.invoke(AbstractTracer.java:84)
         at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1440)
         at sun.net.www.protocol.http.HttpURLConnection.getHeaderField(HttpURLConnection.java:2942)
         at com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTracer.getHeaderValue(HttpURLConnectionTracer.java:21)
         at com.newrelic.agent.tracers.AbstractCrossProcessTracer.getHeader(AbstractCrossProcessTracer.java:43)
         at com.newrelic.agent.HeadersUtil.getAppDataHeader(HeadersUtil.java:79)
         at com.newrelic.agent.CrossProcessTransactionStateImpl.processInboundResponseHeaders(CrossProcessTransactionStateImpl.java:275)
         at com.newrelic.agent.tracers.AbstractCrossProcessTracer.doFinish(AbstractCrossProcessTracer.java:26)
         at com.newrelic.agent.tracers.DefaultTracer.finish(DefaultTracer.java:172)
         at com.newrelic.agent.tracers.AbstractTracer.invoke(AbstractTracer.java:84)
         at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1440)
         at sun.net.www.protocol.https.HttpsURLConnectionImpl.getInputStream(HttpsURLConnectionImpl.java:254)
         at com.sun.jersey.client.urlconnection.URLConnectionClientHandler.getInputStream(URLConnectionClientHandler.java:321)
         at com.sun.jersey.client.urlconnection.URLConnectionClientHandler._invoke(URLConnectionClientHandler.java:260)
         at com.sun.jersey.client.urlconnection.URLConnectionClientHandler.handle(URLConnectionClientHandler.java:153)
         at com.sun.jersey.api.client.filter.GZIPContentEncodingFilter.handle(GZIPContentEncodingFilter.java:120)
         ...

       In other words, the customer's code called HttpURLConnection.getInputStream(), and so does our tracer. So Agent looped,
       killing the instrumented server.

       It is possible, but by no means certain, that replacing the pointcut with weaved instrumentation might resolve this issue.

       NOTE (Jan 2017) - the stack trace above is no longer possible, because AbstractCrossProcessTracer has been removed
       from the product. But it remains unclear whether replacement instrumentation can cause the same kind of problem.
     */
    public void lockTracerStart() {
        --tracerStartLock;
    }

    /**
     * Release the tracer start lock.
     */
    public void unlockTracerStart() {
        ++tracerStartLock;
    }

    /**
     * Test the tracer start lock.
     *
     * @return True if the lock is held. The "lock" is designed to prevent reentrancy by the thread already holding it,
     * so of course it is implemented as a non-reentrant lock.
     */
    public boolean isTracerStartLocked() {
        return tracerStartLock < 0;
    }

    /**
     * Optimized tracer start call. Checks several conditions and if all conditions hold then return true. Note: if a
     * transaction is over its segment limit, we still create tracers, but don't start them. So we don't check the
     * segment limit here.
     *
     * @return True if can create a tracer on this thread at this time else false. If the return value is true, the
     * tracerStartLock is held and the caller is responsible for later calling unlockTracerStart().
     */
    public boolean checkTracerStart() {
        if (isTracerStartLocked()) {
            return false;
        }
        if (!isLeaf() && !activityIsIgnored) {
            lockTracerStart();
            return true;
        }
        return false;
    }

    /**
     * Get the last tracer on the call stack, or null if the call stack is empty.
     */
    public Tracer getLastTracer() {
        return flyweightInProgress ? FLYWEIGHT_PLACEHOLDER : this.lastTracer;
    }

    public TracedMethod startFlyweightTracer() {
        try {
            if (rootTracer == null || flyweightInProgress || this.lastTracer.isLeaf()) {
                return null;
            }
            flyweightInProgress = true;
            return this.lastTracer;
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, t, "Error starting tracer");
            return null;
        }
    }

    public void finishFlyweightTracer(TracedMethod parent, long startInNanos, long finishInNanos, String className,
            String methodName, String methodDesc, String metricName, String[] rollupMetricNames) {
        try {
            if (parent instanceof DefaultTracer) {
                DefaultTracer parentTracer = (DefaultTracer) parent;

                long duration = finishInNanos - startInNanos;

                if (!flyweightInProgress) {
                    Agent.LOG.log(Level.FINEST, "Error finishing tracer - the last tracer is of the wrong type.");
                }

                if (duration < 0) {
                    Agent.LOG.log(Level.FINEST, "A tracer finished with a negative duration.");
                    return;
                }

                transactionStats.getScopedStats().getOrCreateResponseTimeStats(metricName).recordResponseTimeInNanos(duration);
                if (Agent.isDebugEnabled()) {
                    Agent.LOG.log(Level.FINEST, "Finished flyweight tracer {0} ({1}.{2}{3})", metricName, className,
                            methodName, methodDesc);
                }

                if (rollupMetricNames != null) {
                    SimpleStatsEngine unscopedStats = transactionStats.getUnscopedStats();
                    for (String name : rollupMetricNames) {
                        unscopedStats.getOrCreateResponseTimeStats(name).recordResponseTimeInNanos(duration);
                    }
                }

                parentTracer.childTracerFinished(duration);
            }
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, t, "Error finishing tracer");
        } finally {
            flyweightInProgress = false;
        }
    }

    /**
     * This is a placeholder that we set as the current tracer whenever a flyweight tracer is started.
     */
    private static final Tracer FLYWEIGHT_PLACEHOLDER = new NoOpTracer();

    public void startAsyncActivity(Transaction transaction, int activityId, Tracer parentTracer) {
        this.transaction = transaction;
        this.activityId = activityId;
        transaction.activityStarted(this);

        startAsyncTracerLimitCleanup();

        if (parentTracer != null) {
            rootTracer.setParentTracer(parentTracer);
        } else {
            // We worked hard to prevent this case by doing extra checking on the parent tracer
            // back in the register method. So this means something completely weird happened.
            // Fancy recovery is messy here, so we don't bother.
            Agent.LOG.log(Level.FINE, "TransactionActivity.startAsyncActivity: parentTracer is null.");
        }
    }

    void startAsyncTracerLimitCleanup() {
        if (canCreateTransactionSegment()) {
            int numTracers = tracers != null ? tracers.size() : 0;
            // add one for the root
            transaction.getTransactionCounts().addTracers(numTracers + 1);
        } else {
            /*
             * If we get here, then we are already over the transaction segment limit. Mark this entire txa as metrics
             * only. We are currently doing this by marking the root tracer. I am not bother to change all of the other
             * tracers. They will think they are still transaction segments.
             */
            rootTracer.removeTransactionSegment();
            // we can drop the current tracer list
            // we are never going to add another tracer to this and so get rid of the list
            tracers = Collections.emptyList();
        }
    }

    public Tracer getRootTracer() {
        return rootTracer;
    }

    /**
     * Get a cache to store objects for the life of the transaction.
     */
    public TransactionCache getTransactionCache() {
        return transactionCache;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public int hashCode() {
        return activityId;
    }

}
