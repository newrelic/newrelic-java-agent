/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.internal;

import com.newrelic.agent.config.EnvironmentFacade;

import java.util.Collections;
import java.util.Map;

public class NoOpEnvironmentFacade extends EnvironmentFacade {
    @Override
    public String getenv(String key) {
        return null;
    }

    @Override
    public Map<String, String> getAllEnvProperties() {
        return Collections.emptyMap();
    }
}
