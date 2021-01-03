/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.InfiniteTracing;
import com.newrelic.agent.AgentConnectionEstablishedListener;
import com.newrelic.agent.service.analytics.InfiniteTracingEnabledCheck;

import java.util.Map;

class UpdateInfiniteTracingAfterConnect implements AgentConnectionEstablishedListener {

    private final InfiniteTracingEnabledCheck infiniteTracingEnabledCheck;
    private final InfiniteTracing infiniteTracing;

    public UpdateInfiniteTracingAfterConnect(InfiniteTracingEnabledCheck infiniteTracingEnabledCheck,
            InfiniteTracing infiniteTracing) {
        this.infiniteTracingEnabledCheck = infiniteTracingEnabledCheck;
        this.infiniteTracing = infiniteTracing;
    }

    @Override
    public void onEstablished(String appName, String agentRunToken, Map<String, String> requestMetadata) {
        if (!infiniteTracingEnabledCheck.isEnabledAndSpanEventsEnabled()) {
            return;
        }
        infiniteTracing.stop();
        infiniteTracing.start(agentRunToken, requestMetadata);
    }

}
