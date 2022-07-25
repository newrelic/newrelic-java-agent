/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

/**
 * The internal bridge version of TracedMethod.
 */
public interface TracedMethod extends com.newrelic.api.agent.TracedMethod {

    /**
     * Returns the parent of this traced method, or null if this is the root tracer.
     *
     * @return the parent TracedMethod of the current traced method.
     */
    TracedMethod getParentTracedMethod();

    void setRollupMetricNames(String... metricNames);

    /**
     * Sets the traced method metric name, transaction segment name, and transaction segment URI.
     */
    void setMetricNameFormatInfo(String metricName, String transactionSegmentName, String transactionSegmentUri);

    /**
     * Add a rollup metric name that reports the exclusive time for <b>both</b> total and exclusive times.
     *
     * The reason for this is that external and database charts on APM show stacked graphs based on total duration of
     * our rollup metrics, when the intent seems to be to show stacked graphs of exclusive durations.  Previous tracer
     * implementations like AbstractExternalComponentTracer called ResponseTimeStats.recordResponseTime(
     * getExclusiveDuration(), TimeUnit.NANOSECONDS), presumably to "enable" this "feature".  So this method only exists
     * in the bridge API to replicate the old behavior.
     *
     * More investigation is necessary.  Use with care.
     *
     * @param metricNameParts The segments of the metric name. These values will be concatenated together separated by a
     *        `/` char.
     */
    void addExclusiveRollupMetricName(String ... metricNameParts);

    /**
     * Names the current transaction using this traced method. This is called by code injected as a result of xml
     * instrumentation. This probably shouldn't be invoked directly.
     *
     * @param namePriority The priority to be given to the naming call.
     */
    void nameTransaction(TransactionNamePriority namePriority);

    /**
     * Returns true if this tracer produces a metric.
     *
     * @return
     */
    boolean isMetricProducer();

    /**
     * Set the prefix for the metric name.
     */
    void setCustomMetricPrefix(String prefix);

    /**
     * Tell the tracer to track child async jobs which are submitted under its method call.
     * This only applies to the scala instrumentation at the moment.
     */
    public void setTrackChildThreads(boolean shouldTrack);

    public boolean trackChildThreads();

    /**
     * Tell the tracer to track child CallbackRunnable jobs which are submitted under its method call.
     * This only applies to the Akka-Http instrumentation at the moment.
     */
    public void setTrackCallbackRunnable(boolean shouldTrack);

    public boolean isTrackCallbackRunnable();

    /**
     * Do not use. Use
     * {@link com.newrelic.api.agent.TracedMethod#addOutboundRequestHeaders(OutboundHeaders)} instead.
     *
     * To be called when performing an outbound external request using HTTP or JMS. This method must be called before
     * any headers are written to the output stream. This method is generally used in conjunction with reportAsExternal.
     *
     * @param outboundHeaders The headers that will be written to the output stream for the external request. This also
     *        determines if the external call is HTTP or JMS.
     * @since 3.26.0
     */
    @Deprecated
    void addOutboundRequestHeaders(OutboundHeaders outboundHeaders);

    /**
     * Do not use. There is no direct replacement. Use the public cross application tracing API if possible or use the
     * bridge method {@link com.newrelic.agent.bridge.Transaction#provideHeaders(InboundHeaders)} if necessary.
     *
     * @param inboundResponseHeaders do not use.
     * @since 3.26.0
     */
    @Deprecated
    void readInboundResponseHeaders(InboundHeaders inboundResponseHeaders);

    /**
     * @Deprecated Do not use. Use
     * {@link com.newrelic.api.agent.TracedMethod#reportAsExternal(ExternalParameters)} instead.
     *
     * Used to report this traced method as an HTTP external call, datastore external call, or general external call.
     * Use {@link com.newrelic.api.agent.ExternalParametersFactory} to create the input externalParameters. If you are performing an external
     * HTTP call, be sure to call addOutboundRequestHeaders prior to the request being sent.
     *
     * @param externalParameters The appropriate input parameters depending on the type external. Use the
     *        {@link com.newrelic.api.agent.ExternalParametersFactory} to create input parameters. For example,
     *        {@link com.newrelic.api.agent.ExternalParametersFactory}'s createForDatastore to report this TracedMethod as a datastore.
     * @since 3.26.0
     */
    @Deprecated
    void reportAsExternal(com.newrelic.agent.bridge.external.ExternalParameters externalParameters);

}
