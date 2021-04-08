/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.internal;

import com.newrelic.agent.config.SystemProps;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class MapSystemProps extends SystemProps {

    private final Properties properties;

    public MapSystemProps(Map<String, String> props) {
        this.properties = new Properties();
        for (Entry<String, String> entry : props.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String getSystemProperty(String prop) {
        return properties.getProperty(prop);
    }

    @Override
    public Properties getAllSystemProperties() {
        return properties;
    }
}
