/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.service.module;

import com.newrelic.agent.AgentConnectionEstablishedListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class JarCollectorConnectionListener implements AgentConnectionEstablishedListener {
    private final String defaultAppName;
    private final AtomicBoolean shouldReset;

    public JarCollectorConnectionListener(String defaultAppName, AtomicBoolean shouldReset) {
        this.defaultAppName = defaultAppName;
        this.shouldReset = shouldReset;
    }

    @Override
    public void onEstablished(String appName, String agentRunToken, Map<String, String> requestMetadata) {
        if (!appName.equals(defaultAppName)) {
            return;
        }

        shouldReset.set(true);
    }
}
