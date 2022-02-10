/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

import static com.newrelic.agent.config.AgentConfigImpl.LOG_SENDING;

public class LogSenderConfigImpl extends BaseConfig implements LogSenderConfig {
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config." + LOG_SENDING + ".";
    public static final String ENABLED_PROP = "enabled";
    public static final String MAX_SAMPLES_STORED_PROP = "max_samples_stored";
    public static final int DEFAULT_MAX_SAMPLES_STORED = 10000;
    public static final boolean DEFAULT_ENABLED = true; // TODO make off by default, add yaml config
    public static final String ENABLED = SYSTEM_PROPERTY_ROOT + ENABLED_PROP;

    /* log_sending:
     *   forwarding:
     *     enabled: true
     *     max_samples_stored: 10000
     *   decorating:
     *     enabled: true
     */

    public final int maxSamplesStored;
    public final boolean isEnabled;

    public LogSenderConfigImpl(Map<String, Object> pProps, boolean highSecurity) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        maxSamplesStored = getProperty(MAX_SAMPLES_STORED_PROP, DEFAULT_MAX_SAMPLES_STORED);
        isEnabled = !highSecurity && initEnabled();
    }

    public boolean initEnabled() {
        boolean storedMoreThan0 = maxSamplesStored > 0;
        Boolean configEnabled = getProperty(ENABLED_PROP, DEFAULT_ENABLED);
        return storedMoreThan0 && configEnabled;
    }

    static LogSenderConfigImpl createLogSenderConfig(Map<String, Object> settings, boolean highSecurity) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new LogSenderConfigImpl(settings, highSecurity);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

}
