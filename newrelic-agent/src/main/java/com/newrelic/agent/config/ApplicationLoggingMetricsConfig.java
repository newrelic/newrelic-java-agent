/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class ApplicationLoggingMetricsConfig extends BaseConfig {
    public static final String ROOT = "metrics";
    public static final String ENABLED = "enabled";
    public static final boolean DEFAULT_ENABLED = false;

    private final boolean enabled;

    public ApplicationLoggingMetricsConfig(Map<String, Object> props, String parentRoot) {
        super(props, parentRoot + ROOT + ".");
        enabled = getProperty(ENABLED, DEFAULT_ENABLED);
    }

    public boolean getEnabled() {
        return enabled;
    }
}
