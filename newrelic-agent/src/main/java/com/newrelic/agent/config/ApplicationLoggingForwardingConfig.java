/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.util.Map;
import java.util.logging.Level;

public class ApplicationLoggingForwardingConfig extends BaseConfig {
    public static final String ROOT = "forwarding";
    public static final String ENABLED = "enabled";
    public static final String MAX_SAMPLES_STORED = "max_samples_stored";

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_MAX_SAMPLES_STORED = 10000;

    private final boolean enabled;
    private final int maxSamplesStored;

    public ApplicationLoggingForwardingConfig(Map<String, Object> props, String parentRoot, boolean highSecurity) {
        super(props, parentRoot + ROOT + ".");
        maxSamplesStored = initMaxSamplesStored();
        boolean storedMoreThan0 = maxSamplesStored > 0;
        enabled = storedMoreThan0 && !highSecurity && getProperty(ENABLED, DEFAULT_ENABLED);
    }

    private int initMaxSamplesStored() {
        try {
            return getProperty(MAX_SAMPLES_STORED, DEFAULT_MAX_SAMPLES_STORED);
        } catch (ClassCastException classCastException) {
            Agent.LOG.log(Level.WARNING, "The max_samples_stored was likely too large {0}, we will use default {1}",
                    getProperty(MAX_SAMPLES_STORED), DEFAULT_MAX_SAMPLES_STORED);
            return DEFAULT_MAX_SAMPLES_STORED;
        }
    }

    public boolean getEnabled() {
        return enabled;
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }
}
