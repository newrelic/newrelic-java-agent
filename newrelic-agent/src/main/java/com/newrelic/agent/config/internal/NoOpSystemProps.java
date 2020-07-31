/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.internal;

import com.newrelic.agent.config.SystemProps;

import java.util.Properties;

public class NoOpSystemProps extends SystemProps {

    @Override
    public String getSystemProperty(String prop) {
        return null;
    }

    @Override
    public Properties getAllSystemProperties() {
        return new Properties();
    }
}
