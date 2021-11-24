/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.api.agent.Logger;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CollectorSpanEventReservoirManagerTest {
    private  final String APP_NAME = "blerg";

    @Test
    public void maxSamplesStoredConfiguredAfterConstruction() {
        ConfigService mockConfigService = mock21Samples();
        CollectorSpanEventReservoirManager target = new CollectorSpanEventReservoirManager(mockConfigService);
        assertEquals(21, target.getMaxSamplesStored());
    }

    @Test
    public void getOrCreateReturnsSameReservoir() {
        ConfigService mockConfigService = mock21Samples();
        CollectorSpanEventReservoirManager target = initWith25Tries(mockConfigService);
        assertEquals(21, target.getOrCreateReservoir(APP_NAME).size());
        assertTrue(target.getOrCreateReservoir(APP_NAME).isFull());
    }

    @Test
    public void attemptToSendDoesSend() {
        ConfigService mockConfigService = mock21Samples();

        CollectorSpanEventReservoirManager target = initWith25Tries(mockConfigService);

        // boolean arrays can be final so that nested classes can use them
        final boolean[] resultFlags = new boolean[] { false };
        ReservoirManager.HarvestResult harvestResult = target.attemptToSendReservoir(
                APP_NAME,
                (appName, reservoirSize, eventsSeen, events) -> {
                    assertEquals(APP_NAME, appName);
                    assertEquals(21, reservoirSize);
                    assertEquals(25, eventsSeen);
                    assertEquals(21, events.size());
                    resultFlags[0] = true;
                },
                mock(Logger.class)
        );

        assertTrue("It doesn't seem like the sendEvents call succeeded.", resultFlags[0]);
        assertEquals(25, harvestResult.seen);
        assertEquals(21, harvestResult.sent);
    }

    @Test
    public void retryOnHttpErrorWithNoDiscard() {
        ConfigService mockConfigService = mock21Samples();

        CollectorSpanEventReservoirManager target = initWith25Tries(mockConfigService);

        ReservoirManager.HarvestResult harvestResult = target.attemptToSendReservoir(
                APP_NAME,
                (appName, reservoirSize, eventsSeen, events) -> {
                    throw new HttpError("don't discard", 429, 1234);
                },
                mock(Logger.class)
        );

        assertNull(harvestResult);
        assertEquals(21, target.getOrCreateReservoir(APP_NAME).size());
        assertEquals(21, target.getOrCreateReservoir(APP_NAME).getNumberOfTries());
    }

    @Test
    public void httpErrorTriggerDiscard() {
        ConfigService mockConfigService = mock21Samples();

        CollectorSpanEventReservoirManager target = initWith25Tries(mockConfigService);

        ReservoirManager.HarvestResult harvestResult = target.attemptToSendReservoir(
                APP_NAME,
                (appName, reservoirSize, eventsSeen, events) -> {
                    throw new HttpError("message", 0, 0) {
                        @Override
                        public boolean discardHarvestData() {
                            return true;
                        }
                    };
                },
                mock(Logger.class)
        );

        assertNull(harvestResult);
        assertEquals(0, target.getOrCreateReservoir(APP_NAME).size());
        assertEquals(0, target.getOrCreateReservoir(APP_NAME).getNumberOfTries());
    }

    @Test
    public void exceptionTriggerDiscard() {
        ConfigService mockConfigService = mock21Samples();

        CollectorSpanEventReservoirManager target = initWith25Tries(mockConfigService);

        ReservoirManager.HarvestResult harvestResult = target.attemptToSendReservoir(
                APP_NAME,
                (appName, reservoirSize, eventsSeen, events) -> {
                    throw new RuntimeException("~~ oops ~~");
                },
                mock(Logger.class)
        );

        assertNull(harvestResult);
        assertEquals(0, target.getOrCreateReservoir(APP_NAME).size());
        assertEquals(0, target.getOrCreateReservoir(APP_NAME).getNumberOfTries());
    }

    @Test
    public void doesNotSendWithEmptyReservoir() {
        ConfigService mockConfigService = mock21Samples();

        CollectorSpanEventReservoirManager target = new CollectorSpanEventReservoirManager(mockConfigService);

        final boolean[] wasSent = new boolean[] { false };
        ReservoirManager.HarvestResult harvestResult = target.attemptToSendReservoir(
                APP_NAME,
                (appName, reservoirSize, eventsSeen, events) -> wasSent[0] = true,
                mock(Logger.class)
        );

        assertNull(harvestResult);
        assertFalse(wasSent[0]);
    }

    public ConfigService mock21Samples() {
        ConfigService mockConfigService = Mockito.mock(ConfigService.class);
        AgentConfig mockConfig = Mockito.mock(AgentConfig.class);
        SpanEventsConfig mockSpanEventsConfig = Mockito.mock(SpanEventsConfig.class);

        when(mockConfigService.getDefaultAgentConfig()).thenReturn(mockConfig);
        when(mockConfigService.getAgentConfig(anyString())).thenReturn(mockConfig);
        when(mockConfig.getSpanEventsConfig()).thenReturn(mockSpanEventsConfig);
        when(mockSpanEventsConfig.getMaxSamplesStored()).thenReturn(21);
        return mockConfigService;
    }

    public CollectorSpanEventReservoirManager initWith25Tries(ConfigService mockConfigService) {
        CollectorSpanEventReservoirManager target = new CollectorSpanEventReservoirManager(mockConfigService);
        for (int i = 0; i < 25; i++) {
            target.getOrCreateReservoir(APP_NAME).add(SpanEvent.builder().priority(i).build());
        }
        return target;
    }
}