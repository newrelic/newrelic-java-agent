/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

/**
 * Interface for cloud configuration options.
 */

public interface CloudConfig extends Config {
    boolean isCloudMetadataProxyBypassEnabled();
}
