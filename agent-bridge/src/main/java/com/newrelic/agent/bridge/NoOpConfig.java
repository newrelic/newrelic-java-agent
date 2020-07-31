/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Config;

public class NoOpConfig implements Config {

    public static final Config Instance = new NoOpConfig();

    @Override
    public <T> T getValue(String prop) {
        return null;
    }

    @Override
    public <T> T getValue(String key, T defaultVal) {
        return defaultVal;
    }

}
