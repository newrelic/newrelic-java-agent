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
    private final AtomicBoolean shouldSendAllJars;

    public JarCollectorConnectionListener(String defaultAppName, AtomicBoolean shouldSendAllJars) {
        this.defaultAppName = defaultAppName;
        this.shouldSendAllJars = shouldSendAllJars;
    }

    @Override
    public void onEstablished(String appName, String agentRunToken, Map<String, String> requestMetadata) {
        if (!appName.equals(defaultAppName)) {
            return;
        }

        shouldSendAllJars.set(true);
    }
}
