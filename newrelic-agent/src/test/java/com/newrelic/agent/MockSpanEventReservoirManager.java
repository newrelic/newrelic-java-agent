/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ComparisonChain;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.api.agent.Logger;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

public class MockSpanEventReservoirManager implements ReservoirManager<SpanEvent> {
    private final ConfigService configService;
    private ConcurrentHashMap<String, SamplingPriorityQueue<SpanEvent>> spanReservoirsForApp = new ConcurrentHashMap<>();
    private volatile int maxSamplesStored;

    public MockSpanEventReservoirManager(ConfigService configService) {
        this.configService = configService;
        this.maxSamplesStored = configService.getDefaultAgentConfig().getSpanEventsConfig().getMaxSamplesStored();
    }

    @Override
    public SamplingPriorityQueue<SpanEvent> getOrCreateReservoir(String appName) {
        SamplingPriorityQueue<SpanEvent> reservoir = spanReservoirsForApp.get(appName);
        if (reservoir == null) {
            reservoir = spanReservoirsForApp.putIfAbsent(appName, createDistributedSamplingReservoir(appName));
            if (reservoir == null) {
                reservoir = spanReservoirsForApp.get(appName);
            }
        }
        return reservoir;
    }

    private SamplingPriorityQueue<SpanEvent> createDistributedSamplingReservoir(String appName) {
        SpanEventsConfig spanEventsConfig = configService.getDefaultAgentConfig().getSpanEventsConfig();
        return new DistributedSamplingPriorityQueue<>(appName, "Span Event Service", maxSamplesStored, SPAN_EVENT_COMPARATOR);
    }


    @Override
    public void clearReservoir() {
        spanReservoirsForApp.clear();
    }

    @Override
    public HarvestResult attemptToSendReservoir(String appName, EventSender<SpanEvent> eventSender, Logger logger) {
        return null;
    }

    @Override
    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    @Override
    public void setMaxSamplesStored(int newMax) {
        maxSamplesStored = newMax;
        ConcurrentHashMap<String, SamplingPriorityQueue<SpanEvent>> newMaxSpanReservoirs = new ConcurrentHashMap<>();
        spanReservoirsForApp.forEach((appName,reservoir ) -> newMaxSpanReservoirs.putIfAbsent(appName, createDistributedSamplingReservoir(appName)));
        spanReservoirsForApp = newMaxSpanReservoirs;
    }

    // This is where you can add secondary sorting for Span Events
    private static final Comparator<SpanEvent> SPAN_EVENT_COMPARATOR = new Comparator<SpanEvent>() {
        @Override
        public int compare(SpanEvent left, SpanEvent right) {
            return ComparisonChain.start()
                    .compare(right.getPriority(), left.getPriority()) // Take highest priority first
                    .result();
        }
    };

}
