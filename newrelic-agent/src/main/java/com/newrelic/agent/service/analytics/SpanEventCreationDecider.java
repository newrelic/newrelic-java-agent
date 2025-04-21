/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

public class SpanEventCreationDecider {

    private final ConfigService configService;

    public SpanEventCreationDecider(ConfigService configService) {
        this.configService = configService;
    }

    public boolean shouldCreateSpans(TransactionData transactionData) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "*SpanEvent* shouldCreateSpans- infinite tracing enabled: {0}   transaction sampled {1}",
                configService.getDefaultAgentConfig().getInfiniteTracingConfig().isEnabled(), transactionData.sampled());
        return configService.getDefaultAgentConfig().getInfiniteTracingConfig().isEnabled()
                || transactionData.sampled();
    }
}
