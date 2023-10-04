/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class SlowTransactionsConfigImpl extends BaseConfig implements SlowTransactionsConfig {

    public static final String ROOT = "slow_transactions";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config." + ROOT + ".";
    public static final String ENABLED = "enabled";
    public static final String THRESHOLD = "threshold";

    public static final boolean DEFAULT_ENABLED = false;
    public static final int DEFAULT_THRESHOLD_MILLIS = 1000;

    private final boolean isEnabled;
    private final int thresholdMillis;

    public SlowTransactionsConfigImpl(Map<String, Object> pProps) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        thresholdMillis = getIntProperty(THRESHOLD, DEFAULT_THRESHOLD_MILLIS);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public long getThresholdMillis() {
        return thresholdMillis;
    }

}
