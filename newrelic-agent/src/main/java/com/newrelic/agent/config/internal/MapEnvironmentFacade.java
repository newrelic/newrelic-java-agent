/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.internal;

import com.newrelic.agent.config.EnvironmentFacade;

import java.util.Map;

public class MapEnvironmentFacade extends EnvironmentFacade {
    private final Map<String, String> map;

    public MapEnvironmentFacade(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public String getenv(String key) {
        return map.get(key);
    }

    @Override
    public Map<String, String> getAllEnvProperties() {
        return map;
    }
}
