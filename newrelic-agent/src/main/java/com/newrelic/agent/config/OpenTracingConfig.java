/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class OpenTracingConfig extends BaseConfig {

    private static final boolean DEFAULT_OPEN_TRACING_ENABLED = false;
    private static final String ENABLED = "enabled";
    private static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.open_tracing.";

    private final boolean enabled;

    public OpenTracingConfig(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        this.enabled = getProperty(ENABLED, DEFAULT_OPEN_TRACING_ENABLED);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
