/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.Map;

import com.newrelic.api.agent.Insights;

class NoOpInsights implements Insights {
    static final Insights INSTANCE = new NoOpInsights();

    private NoOpInsights() {
    }

    @Override
    public void recordCustomEvent(String eventType, Map<String, ?> attributes) {
    }

}
