/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.SuperAgentIntegrationConfig;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SuperAgentIntegrationUtilsTest {
    private HealthDataChangeListener mockListener;

    @Before
    public void before() {
        mockListener = mock(HealthDataChangeListener.class);

        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);
    }

    @Test
    public void getPseudoCurrentTimeNanos_returnsTimeInNanos() {
        long testTime = System.currentTimeMillis();
        long time = SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos();
        assertTrue(time >= testTime * 1000000);
    }

    @Test
    public void reportUnhealthyStatus_updatesHealthListeners() {
        SuperAgentIntegrationUtils.reportUnhealthyStatus(Collections.singletonList(mockListener), AgentHealth.Status.GC_CIRCUIT_BREAKER);
        verify(mockListener).onUnhealthyStatus(AgentHealth.Status.GC_CIRCUIT_BREAKER);
    }

    @Test
    public void reportHealthyStatus_updatesHealthListeners() {
        SuperAgentIntegrationUtils.reportHealthyStatus(Collections.singletonList(mockListener), AgentHealth.Category.HARVEST);
        verify(mockListener).onHealthyStatus(AgentHealth.Category.HARVEST);
    }
}
