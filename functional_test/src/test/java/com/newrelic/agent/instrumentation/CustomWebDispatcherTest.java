/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.util.Set;

import org.junit.Test;

import com.newrelic.agent.AgentHelper;

public class CustomWebDispatcherTest {

    @Test
    public void test() {
        new CustomWebDispatcher().dispatch();

        Set<String> metrics = AgentHelper.getMetrics();

        AgentHelper.verifyMetrics(metrics, "WebTransaction", "WebTransactionTotalTime", "HttpDispatcher",
                "WebTransactionTotalTime/Custom/com.newrelic.agent.instrumentation.CustomWebDispatcher/dispatch",
                "WebTransaction/Custom/com.newrelic.agent.instrumentation.CustomWebDispatcher/dispatch",
                "Custom/com.newrelic.agent.instrumentation.CustomWebDispatcher/foo");
    }
}
