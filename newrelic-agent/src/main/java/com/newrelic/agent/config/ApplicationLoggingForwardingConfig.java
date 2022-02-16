/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class ApplicationLoggingForwardingConfig extends BaseConfig {
    public static final String ROOT = "forwarding";
    public static final String ENABLED = "enabled";
    public static final String MAX_SAMPLES_STORED = "max_samples_stored";

    public static final boolean DEFAULT_ENABLED = false;
    public static final int DEFAULT_MAX_SAMPLES_STORED = 2000;

    private final boolean enabled;
    private final int maxSamplesStored;

    public ApplicationLoggingForwardingConfig(Map<String, Object> props, String parentRoot, boolean highSecurity) {
        super(props, parentRoot + ROOT + ".");
        maxSamplesStored = getProperty(MAX_SAMPLES_STORED, DEFAULT_MAX_SAMPLES_STORED);
        boolean storedMoreThan0 = maxSamplesStored > 0;
        enabled = storedMoreThan0 && !highSecurity && getProperty(ENABLED, DEFAULT_ENABLED);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }
}
