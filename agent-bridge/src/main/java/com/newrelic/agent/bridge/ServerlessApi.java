/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

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
}
