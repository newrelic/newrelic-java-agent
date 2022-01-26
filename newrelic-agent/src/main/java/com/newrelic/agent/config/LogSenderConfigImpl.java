/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class LogSenderConfigImpl extends BaseConfig implements LogSenderConfig {
    public static final String MAX_SAMPLES_STORED_PROP = "max_samples_stored";
    public static final int DEFAULT_MAX_SAMPLES_STORED = 10000;
    public static final String ENABLED_PROP = "enabled";
    public static final boolean DEFAULT_ENABLED = true; // TODO make off by default, add yaml config
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.log_sending.";
    public static final String ENABLED = SYSTEM_PROPERTY_ROOT + ENABLED_PROP;
//    public static final String COLLECT_CUSTOM_EVENTS = "collect_custom_events";

    public final int maxSamplesStored;
    public final boolean isEnabled;

    public LogSenderConfigImpl(Map<String, Object> pProps, boolean highSecurity) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        maxSamplesStored = getProperty(MAX_SAMPLES_STORED_PROP, DEFAULT_MAX_SAMPLES_STORED);
        // TODO ignores highSecurity for the time being. Should we respect it?
//        isEnabled = !highSecurity && initEnabled();
        isEnabled = initEnabled();
    }

    public boolean initEnabled() {
        boolean storedMoreThan0 = maxSamplesStored > 0;
        Boolean configEnabled = getProperty(ENABLED_PROP, DEFAULT_ENABLED);
//        /*
//         * "collect_analytics_events" is the property which comes down from the server. This gets mapped to
//         * transaction_events.collect_analytics_events in AgentConfigFactory.mergeServerData()
//         */
//        Boolean featureGateEnabled = getProperty(COLLECT_CUSTOM_EVENTS, DEFAULT_ENABLED);
//        return storedMoreThan0 && configEnabled && featureGateEnabled;
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
