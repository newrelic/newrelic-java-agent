/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.InfiniteTracingConfig;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.interfaces.backport.Consumer;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.analytics.SpanEventsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SpanEventsServiceFactoryTest {
    private final String APP_NAME = "fakeyMcFakeApp";

    SpanEvent event;
    @Mock
    ConfigService configService;
    @Mock
    RPMServiceManager rpmServiceManager;
    @Mock
    Consumer<SpanEvent> infTracingConsumer;
    @Mock
    AgentConfig agentConfig;
    @Mock
    InfiniteTracingConfig infiniteTracingConfig;
    @Mock
    SpanEventsConfig spanEventsConfig;
    @Mock
    TransactionService transactionService;
    @Mock
    ReservoirManager<SpanEvent> reservoirManager;
    @Mock
    SamplingPriorityQueue<SpanEvent> reservoir;

    @Before
    public void setup() {
        event = SpanEvent.builder().putAgentAttribute("foo", "spoon").build();

        MockitoAnnotations.initMocks(this);

        when(configService.getDefaultAgentConfig()).thenReturn(agentConfig);
        when(agentConfig.getSpanEventsConfig()).thenReturn(spanEventsConfig);
        when(agentConfig.getInfiniteTracingConfig()).thenReturn(infiniteTracingConfig);
        when(reservoirManager.getMaxSamplesStored()).thenReturn(90210);
        when(reservoirManager.getOrCreateReservoir(APP_NAME)).thenReturn(reservoir);
    }

    @Test
    public void testBuildPicksStorageBackend_collector() throws Exception {

        when(agentConfig.getApplicationName()).thenReturn(APP_NAME);
        when(infiniteTracingConfig.isEnabled()).thenReturn(false);
        when(spanEventsConfig.isEnabled()).thenReturn(true);
        when(reservoirManager.getOrCreateReservoir(any())).thenReturn(reservoir);

        SpanEventsService service = SpanEventsServiceFactory.builder()
                .configService(configService)
                .rpmServiceManager(rpmServiceManager)
                .infiniteTracingConsumer(infTracingConsumer)
                .transactionService(transactionService)
                .reservoirManager(reservoirManager)
                .build();

        service.storeEvent(event);

        verify(reservoir).add(event);
        verify(infTracingConsumer, never()).accept(event);

        ArgumentCaptor<AgentConfigListener> configListener = ArgumentCaptor.forClass(AgentConfigListener.class);
        ArgumentCaptor<TransactionListener> transactionListener = ArgumentCaptor.forClass(TransactionListener.class);
        verify(configService, times(2)).addIAgentConfigListener(configListener.capture());
        verify(transactionService).addTransactionListener(transactionListener.capture());
        assertSame(configListener.getValue(), service);
        assertSame(transactionListener.getValue(), service);
    }

    @Test
    public void testBuildPicksStorageBackend_infiniteTracing() throws Exception {

        when(agentConfig.getApplicationName()).thenReturn(APP_NAME);
        when(infiniteTracingConfig.isEnabled()).thenReturn(true);
        when(spanEventsConfig.isEnabled()).thenReturn(true);

        SpanEventsService service = SpanEventsServiceFactory.builder()
                .configService(configService)
                .rpmServiceManager(rpmServiceManager)
                .infiniteTracingConsumer(infTracingConsumer)
                .transactionService(transactionService)
                .build();

        service.storeEvent(event);

        verify(infTracingConsumer).accept(event);
        verify(reservoir, never()).add(event);

        ArgumentCaptor<AgentConfigListener> configListener = ArgumentCaptor.forClass(AgentConfigListener.class);
        ArgumentCaptor<TransactionListener> transactionListener = ArgumentCaptor.forClass(TransactionListener.class);
        verify(configService, times(2)).addIAgentConfigListener(configListener.capture());
        verify(transactionService).addTransactionListener(transactionListener.capture());
        assertSame(configListener.getValue(), service);
        assertSame(transactionListener.getValue(), service);
    }
}