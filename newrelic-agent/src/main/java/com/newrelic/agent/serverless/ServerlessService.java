/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.serverless;

import java.util.function.Consumer;

public interface ServerlessService {

    /**
     * Store the function ARN/identifier for the current invocation.
     * This value will be included in the serverless payload metadata.
     *
     * @param arn The function ARN (AWS Lambda)
     */
    void setArn(String arn);

    /**
     * Store the function version for the current invocation.
     * This value will be included in the serverless payload metadata.
     *
     * @param functionVersion The function version
     */
    void setFunctionVersion(String functionVersion);

    /**
     * Retrieve the stored function ARN/identifier.
     * Used by DataSenderServerlessImpl to construct the serverless payload envelope.
     *
     * @return The stored ARN, or null if not set
     */
    String getArn();

    /**
     * Retrieve the stored function version.
     * Used by DataSenderServerlessImpl to construct the serverless payload envelope.
     *
     * @return The stored function version, or null if not set
     */
    String getFunctionVersion();

    /**
     * @return True if APM mode is enabled for this lambda using the NEW_RELIC_APM_LAMBDA_MODE environment variable, otherwise false
     */
    boolean isApmLambdaModeEnabled();

    /**
     * @return True if open telemetry metric collectors are registered
     */
    boolean otelMetricsRegistered();

    /**
     * @return The open telemetry dimensional metric base64 payload. Outside of harvest cycles, this method will return a null value.
     */
    String otelMetricsPayload();

    /**
     * When the Open Telemetry SDK is instrumented in serverless mode,
     * this provides a metric reader and the associated consumer method to
     * force the metric reader to begin collecting metrics.
     * This is to trigger the underlying metric exporters of the given metric reader.
     *
     * @param metricReader The metric reader
     *
     * @param metricCollector Consumer function that conumes the above metric reader and triggers the metric reader to begin collection.
     */
    void addMetricReader(Object metricReader, Consumer<Object> metricCollector);

    /**
     * Removes the metric reader when is shuts down.
     *
     * @param metricReader The metric reader to be removed
     */
    void removeMetricCollector(Object metricReader);

    /**
     * Triggers all metric collector Runnables registered into the agent.
     *
     */
    void collectOtelMetrics();

    /**
     * Begins the harvest cycle for the Open Telemetry hybrid agent when serverless mode is enabled.
     *
     * @param metricPayload The base64 payload of open telemetry dimensional metrics
     *
     * @return A boolean indicating if the harvest cycle was triggered.
     */
    boolean otelHarvest(String metricPayload);

}
