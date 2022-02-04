/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class ReservoirAddingSpanEventConsumerTest {

    private ReservoirManager<SpanEvent> reservoirManager;
    private ConfigService configService;
    private SpanEvent event;
    private SamplingPriorityQueue<SpanEvent> reservoir;
    private AgentConfig agentConfig;
    private SpanEventsConfig spanEventsConfig;

    @Before
    public void setup(){
        reservoirManager = mock(ReservoirManager.class);
        reservoir = mock(SamplingPriorityQueue.class);
        configService = mock(ConfigService.class);
        agentConfig = mock(AgentConfig.class);
        spanEventsConfig = mock(SpanEventsConfig.class);
        event = SpanEvent.builder().putAgentAttribute("foo", "burg").build();
        when(configService.getDefaultAgentConfig()).thenReturn(agentConfig);
        when(agentConfig.getSpanEventsConfig()).thenReturn(spanEventsConfig);
    }

    @Test
    public void testConfigEnabled() throws Exception {
        when(spanEventsConfig.isEnabled()).thenReturn(true);
        when(reservoirManager.getMaxSamplesStored()).thenReturn(1024);
        when(reservoirManager.getOrCreateReservoir(any())).thenReturn(reservoir);
        ReservoirAddingSpanEventConsumer testClass = new ReservoirAddingSpanEventConsumer(reservoirManager, configService);
        testClass.accept(event);
        verify(reservoir).add(event);
    }

    @Test
    public void testConfigDisabled() throws Exception {
        when(spanEventsConfig.isEnabled()).thenReturn(false);
        when(reservoirManager.getMaxSamplesStored()).thenReturn(1024);
        ReservoirAddingSpanEventConsumer testClass = new ReservoirAddingSpanEventConsumer(reservoirManager, configService);
        testClass.accept(event);
        verify(reservoir, never()).add(any(SpanEvent.class));
    }

    @Test
    public void testMaxSamplesZero() throws Exception {
        when(spanEventsConfig.isEnabled()).thenReturn(true);
        when(reservoirManager.getMaxSamplesStored()).thenReturn(0);
        ReservoirAddingSpanEventConsumer testClass = new ReservoirAddingSpanEventConsumer(reservoirManager, configService);
        testClass.accept(event);
        verify(reservoir, never()).add(any(SpanEvent.class));
    }

}