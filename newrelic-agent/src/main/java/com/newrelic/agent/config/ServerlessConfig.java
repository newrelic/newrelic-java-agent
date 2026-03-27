/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface ServerlessConfig {
    boolean isEnabled();
    String filePath();

    /**
     * Get the fallback ARN to use if it cannot be obtained from the Lambda Context.
     *
     * @return The configured ARN, or null if not configured
     */
    String getArn();

    /**
     * Get the fallback function version to use if it cannot be obtained from the Lambda Context.
     *
     * @return The configured function version, or null if not configured
     */
    String getFunctionVersion();
}
