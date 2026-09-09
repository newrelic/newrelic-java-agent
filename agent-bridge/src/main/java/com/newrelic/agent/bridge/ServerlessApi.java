/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * API for serverless instrumentation to communicate metadata to the core agent.
 * This interface provides methods to store serverless platform metadata that will be included
 * in the serverless payload envelope.
 */
public interface ServerlessApi {

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
     * Removes the metric reader when is shuts down.
     *
     * @param metricReader The metric reader to be removed
     */
    void removeMetricCollector(Object metricReader);

    /**
     * When the Open Telemetry SDK is instrumented in serverless mode,
     * this provides a metric reader and the associated collector method to
     * trigger the underlying metric exporters of the given metric reader.
     *
     * @param metricReader The metric reader
     *
     * @param metricCollector The metric collector which consumes the above metric reader and triggers its underlying metric exporters.
     */
    void addMetricCollector(Object metricReader, Consumer<Object> metricCollector);

    /**
     * Begins the harvest cycle for the Open Telemetry hybrid agent when serverless mode is enabled.
     * Since multiple metric exporters may be used in the Open Telemetry SDK, the harvest will only trigger if
     * all the ServerlessMetricExporters from the Open Telemetry SDK instrumentation have called this method.
     *
     * @param metricPayloadProvider A supplier that returns a string containing the base64 payload of open telemetry dimensional metrics
     *
     * @return A boolean indicating if the harvest cycle was triggered.
     */
    boolean otelHarvest(Supplier<String> metricPayloadProvider);

}
