/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

/**
 * Interface for AWS configuration options.
 */

public interface AwsConfig extends Config {
    /**
     * If set to true, the agent will bypass use of a proxy when calling the AWS Fargate metadata endpoint.
     */
    boolean isFargateMetadataProxyBypassEnabled();
}
