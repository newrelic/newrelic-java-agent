/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.api.agent.ExternalParameters;

import java.util.Map;
import java.util.Set;

/**
 * A tracer records information about a method invocation - primarily the start and stop time of the invocation. A
 * tracer instance is associated with a single method invocation.
 *
 * Tracers are created by {@link TracerFactory} instances.
 */
public interface Tracer extends TimedItem, ExitTracer, ErrorTracer {

    TransactionActivity getTransactionActivity();

    /**
     * Get the start time of the method invocation in nanoseconds.
     */
    long getStartTime();

    /**
     * Get the start time of the method invocation in milliseconds.
     */
    long getStartTimeInMilliseconds();

    /**
     * Get the end time of the method invocation in nanoseconds.
     */
    long getEndTime();

    /**
     * Get the end time of the method invocation in milliseconds.
     */
    long getEndTimeInMilliseconds();

    /**
     * Get the duration in nanoseconds of the tracer minus the duration of all child tracers. This measures the amount
     * of time spent in the method itself or in untraced method calls.
     */
    @Override
    long getExclusiveDuration();

    /**
     * Returns the elapsed time since the start time of the transaction up until now if the tracer is still running, or
     * the final duration if the tracer has finished.
     */
    long getRunningDurationInNanos();

    /**
     * The metric name of this tracer. Null is an acceptable value if this tracer does not generate a metric.
     */
    @Override
    String getMetricName();

    /**
     * The name of this tracer in a transaction trace segment. Defaults to the metric name.
     */
    String getTransactionSegmentName();

    /**
     * The uri of this tracer in a tranaction trace segment. Defaults to null.
     */
    String getTransactionSegmentUri();

    /**
     * A map of attributes the customer has added to the tracer
     */
    Map<String, Object> getCustomAttributes();

    /**
     * A map of attributes used to store extra information about the invocation (Like the sql statement for a sql
     * tracer). An empty collection will be returned if there are no parameters. Do not use this to add parameters.
     */
    Map<String, Object> getAgentAttributes();

    /**
     * Add some extra information to the invocation (Like the sql statement for a sql tracer).
     */
    void setAgentAttribute(String key, Object value);

    /**
     * Add some extra information to the invocation (Like the sql statement for a sql tracer).
     */
    void setAgentAttribute(String key, Object value, boolean addToSpan);

    /**
     * Remove attribute.
     *
     * @param key attribute to remove
     */
    void removeAgentAttribute(String key);

    /**
     * Returns the specific object for the input key in the map of attributes used to store extra information about the
     * invocation (Like the sql statement for a sql tracer). If the key is not present, then null will be returned.
     */
    Object getAgentAttribute(String key);

    /**
     * Tells the tracer when a child tracer finished. Useful in computing the exclusive time for the tracer.
     */
    void childTracerFinished(Tracer child);

    int getChildCount();

    /**
     * Returns the parent tracer or null if this is the root tracer.
     */
    Tracer getParentTracer();

    void setParentTracer(Tracer tracer);

    /**
     * Does the tracer have any children?
     */
    boolean isParent();

    ClassMethodSignature getClassMethodSignature();

    /**
     * Returns true if this tracer should participate in transaction trace.
     */
    boolean isTransactionSegment();

    /**
     * True means a child has taken a stack trace and therefore this tracer should not take one.
     *
     * @return True if a child has taken a stack trace for the transaction segment, else false.
     */
    boolean isChildHasStackTrace();

    TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
            long startTime, TransactionSegment lastSibling);

    boolean isLeaf();

    boolean isAsync();

    void removeTransactionSegment();

    void markFinishTime();

    String getGuid();

    long getStartTimeInMillis();

    ExternalParameters getExternalParameters();

    /**
     * Returns the set of agent attribute names that are marked to be added to span events.
     * <br><br>
     * <b>Note</b>: <i>Some attributes will be added to spans even if they are not in the returned set.</i>
     */
    Set<String> getAgentAttributeNamesForSpans();
}
