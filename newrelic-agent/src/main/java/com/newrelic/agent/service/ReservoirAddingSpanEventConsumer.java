/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.interfaces.backport.Consumer;
import com.newrelic.agent.model.SpanEvent;

public class ReservoirAddingSpanEventConsumer implements Consumer<SpanEvent> {
    private final ReservoirManager<SpanEvent> reservoirManager;
    private final ConfigService configService;

    public ReservoirAddingSpanEventConsumer(ReservoirManager<SpanEvent> reservoirManager, ConfigService configService) {
        this.reservoirManager = reservoirManager;
        this.configService = configService;
    }

    @Override
    public void accept(SpanEvent spanEvent) {
        if (isSpanEventsEnabled()) {
            String appName = spanEvent.getAppName();
            SamplingPriorityQueue<SpanEvent> reservoir = reservoirManager.getOrCreateReservoir(appName);
            reservoir.add(spanEvent);
        }
    }

    private boolean isSpanEventsEnabled() {
        SpanEventsConfig spanEventsConfig = configService.getDefaultAgentConfig().getSpanEventsConfig();
        return spanEventsConfig.isEnabled() && reservoirManager.getMaxSamplesStored() > 0;
    }
}
