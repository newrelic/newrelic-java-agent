/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;

public class InfiniteTracingEnabledCheck {

    private final ConfigService configService;

    public InfiniteTracingEnabledCheck(ConfigService configService) {
        this.configService = configService;
    }

    public boolean isEnabledAndSpanEventsEnabled() {
        final AgentConfig config = configService.getDefaultAgentConfig();
        return isEnabled() && config.getSpanEventsConfig().isEnabled();
    }

    public boolean isEnabled() {
        final AgentConfig config = configService.getDefaultAgentConfig();
        return config.getInfiniteTracingConfig().isEnabled();
    }
}
