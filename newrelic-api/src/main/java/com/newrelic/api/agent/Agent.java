/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.util.Map;

/**
 * The New Relic Java Agent's API.
 */
public interface Agent {

    /**
     * Returns the current traced method. This can only be invoked within methods that are traced.
     *
     * @return The current method being traced.
     * @see Trace
     * @since 3.9.0
     */
    TracedMethod getTracedMethod();

    /**
     * Returns the current transaction.
     *
     * @return The current transaction.
     * @since 3.9.0
     */
    Transaction getTransaction();

    /**
     * Returns a logger that logs to the New Relic Java agent log file.
     *
     * @return A log where messages can be written to the New Relic Java agent log file.
     * @since 3.9.0
     */
    Logger getLogger();

    /**
     * Returns the agent's configuration.
     *
     * @return The configuration of this Java agent.
     * @since 3.9.0
     */
    Config getConfig();

    /**
     * Returns a metric aggregator that can be used to record metrics that can be viewed through custom dashboards.
     *
     * @return Aggregator used to record metrics for custom dashboards.
     * @since 3.9.0
     */
    MetricAggregator getMetricAggregator();

    /**
     * Provides access to the Insights custom events API.
     *
     * @return Object used to add custom events.
     * @since 3.13.0
     */
    Insights getInsights();

    /**
     * Provides access to the AI Monitoring custom events API.
     *
     * @return Object for recording custom events.
     */
    AiMonitoring getAiMonitoring();

    ErrorApi getErrorApi();

    /**
     * Provides access to the Trace Metadata API for details about the currently executing distributed trace.
     *
     * @return trace metadata API class
     * @since 5.6.0
     */
    TraceMetadata getTraceMetadata();

    /**
     * Returns an opaque map of key/value pairs that can be used to correlate this application in the New Relic backend.
     *
     * @return an opaque map of correlation key/value pairs
     * @since 5.6.0
     */
    Map<String, String> getLinkingMetadata();

}
