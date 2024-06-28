/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.zaxxer.hikari.pool;

import java.util.concurrent.TimeUnit;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariPoolMXBean;

@Weave(originalName = "com.zaxxer.hikari.pool.HikariPool", type = MatchType.ExactClass)
public abstract class HikariPool_Instrumentation implements HikariPoolMXBean {

    public HikariPool_Instrumentation(HikariConfig config) {
        AgentBridge.privateApi.addSampler(new PooledDataSourceSampler(this, config), 5, TimeUnit.SECONDS);
    }
}
