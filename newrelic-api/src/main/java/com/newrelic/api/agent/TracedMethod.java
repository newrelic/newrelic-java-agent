/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * Represents a single instance of the timing mechanism associated with a method that is instrumented using the
 * {@link Trace} annotation.
 * 
 * @see Agent#getTracedMethod()
 */
public interface TracedMethod extends AttributeHolder {

    /**
     * Returns the traced method metric name.
     * 
     * @return The metric named used for this traced method.
     * @since 3.9.0
     */
    String getMetricName();

    /**
     * Sets the traced method metric name by concatenating all given metricNameParts with a '/' separating each part.
     * 
     * @param metricNameParts The segments of the metric name. These values will be concatenated together separated by a
     *        `/` char.
     * @since 3.9.0
     */
    void setMetricName(String... metricNameParts);

    /**
     * Metric names added here will be reported as roll-up metrics. A roll-up metric is an extra unscoped metric (a
     * metric which is not scoped to a specific transaction) that is reported in addition to the normal metric recorded
     * for a traced method. An example of how the agent uses a roll-up metric is the OtherTransaction/all metric. Each
     * background transaction records data to its transaction specific metric and to the OtherTransaction/all roll-up
     * metric which represents all background transactions.
     * 
     * @param metricNameParts The segments of the rollup metric name. These values will be concatenated together
     *        separated by a `/` char.
     * @since 3.9.0
     */
    void addRollupMetricName(String... metricNameParts);

    /**
     * Used to report this traced method as an HTTP external call, datastore external call, or general external call.
     *
     * Depending on the information available to report, use one of the respective builders for the following objects:
     * {@link GenericParameters}, {@link HttpParameters}, {@link DatastoreParameters},
     * {@link MessageProduceParameters}, {@link MessageConsumeParameters}.
     *
     * Note: If you are performing an external HTTP call, be sure to call
     * {@link #addOutboundRequestHeaders(OutboundHeaders)} prior to the request being sent.
     *
     * @param externalParameters The appropriate input parameters depending on the type of external call. Use one of
     * the respective builders for the following objects: {@link GenericParameters}, {@link HttpParameters},
     * {@link DatastoreParameters}, {@link MessageProduceParameters}, {@link MessageConsumeParameters}.
     * @since 3.36.0
     */
    void reportAsExternal(ExternalParameters externalParameters);

    /**
     * To be called when performing an outbound external request using HTTP or JMS. This method must be called before
     * any headers are written to the output stream. This method is generally used in conjunction with reportAsExternal.
     *
     * @param outboundHeaders The headers that will be written to the output stream for the external request. This also
     *        determines if the external call is HTTP or JMS.
     * @since 3.36.0
     * @deprecated Instead, use the Distributed Tracing API {@link Transaction#insertDistributedTraceHeaders(Headers)} to create a
     * distributed tracing payload
     */
    @Deprecated
    void addOutboundRequestHeaders(OutboundHeaders outboundHeaders);

}
