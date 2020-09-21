/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * <p>
 * Represents a timed unit of work. Like a {@link TracedMethod}, reports a single metric,
 * generates a single segment in a transaction trace, and can be reported as an external call. Unlike a TracedMethod,
 * a Segment's timed duration may encompass arbitrary application work; it is not limited to a single method or thread.
 * </p>
 * <p>
 * Timing begins when the instance is created via {@link Transaction#startSegment} and ends when the {@link #end()}
 * or {@link #ignore()} method is called. These calls can be issued from distinct application threads. If a Segment is
 * not explicitly ignored or ended it will be timed out according to the <code>segment_timeout</code> value which is
 * user configurable in the yaml file or by a Java system property.
 * </p>
 * <p>
 * A {@link Segment} will show up in the Transaction Breakdown table, as well as the Transaction Trace page in APM.
 * </p>
 */
public interface Segment extends AttributeHolder {

    /**
     * Sets the metric name by concatenating all given metricNameParts with a '/' separating each part.
     *
     * @param metricNameParts The segments of the metric name. These values will be concatenated together separated by a
     * `/` char.
     * @since 3.37.0
     */
    void setMetricName(String... metricNameParts);

    /**
     * Reports this traced method as an HTTP external call, datastore external call, or generic external call.
     * Use {@link ExternalParameters} to create the externalParameters argument. If you are performing an external
     * HTTP call, be sure to call {@link #addOutboundRequestHeaders(OutboundHeaders)} prior to the request being sent.
     *
     * @param externalParameters The appropriate input parameters depending on the type of external call. See available
     * Builders in {@link ExternalParameters} for more information.
     * @since 3.37.0
     */
    void reportAsExternal(ExternalParameters externalParameters);

    /**
     * Adds headers to the external request so that the request can be recognized on the receiving end.
     *
     * To be called when performing an outbound external request using HTTP or JMS. This method must be called before
     * any headers are written to the output stream. This method is generally used in conjunction with reportAsExternal.
     *
     * @param outboundHeaders The headers that will be written to the output stream for the external request. This also
     * determines if the external call is HTTP or JMS.
     * @since 3.37.0
     */
    void addOutboundRequestHeaders(OutboundHeaders outboundHeaders);

    /**
     * Get the {@link Transaction} of this {@link Segment}.
     *
     * @return The Transaction.
     * @since 3.37.0
     */
    Transaction getTransaction();

    /**
     * Stops tracking the {@link Segment} and does not report it as part of its parent transaction. This method has no
     * effect if the segment has ended. Every Segment instance must be completed by a call to ignore() or
     * to {@link #end()}.
     *
     * @since 3.37.0
     */
    void ignore();

    /**
     * Stops timing the {@link Segment} and complete the execution on this thread. Only the first call to this method
     * will have an effect. Every Segment instance must be completed by a call to {@link #ignore()} or to end()/endAsync().
     *
     * @since 3.37.0
     */
    void end();

    /**
     * Stops timing the {@link Segment} and complete the execution on another thread. This method is preferred over
     * {@link #end()} when the underlying segment is running on a single threaded event loop.
     *
     * Only the first call to this method will have an effect. Every Segment instance
     * must be completed by a call to {@link #ignore()} or to end()/endAsync().
     *
     * @since 5.4.0
     */
    void endAsync();
}
