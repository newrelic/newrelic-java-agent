/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Logs;

import java.util.Map;

class NoOpLogs implements Logs {
    static final Logs INSTANCE = new NoOpLogs();

    private NoOpLogs() {
    }

    @Override
    public void recordLogEvent(Map<String, ?> attributes) {
    }

}
