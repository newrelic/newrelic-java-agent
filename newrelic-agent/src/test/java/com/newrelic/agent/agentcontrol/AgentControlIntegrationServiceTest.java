/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentControlIntegrationConfig;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentControlIntegrationServiceTest {
    AgentConfig mockAgentConfig;
    AgentControlIntegrationConfig mockAgentControlIntegrationConfig;
    RPMServiceManager mockRPMServiceManager;
    AgentHealth mockAgentHealth;

    @Before
    public void before() {
        mockAgentConfig = mock(AgentConfig.class);
        mockAgentControlIntegrationConfig = mock(AgentControlIntegrationConfig.class);
        mockAgentHealth = mock(AgentHealth.class);
        mockRPMServiceManager = mock(RPMServiceManager.class);

        MockServiceManager manager = new MockServiceManager();
        manager.setRPMServiceManager(mockRPMServiceManager);
        ServiceFactory.setServiceManager(manager);

        when(mockAgentConfig.getAgentControlIntegrationConfig()).thenReturn(mockAgentControlIntegrationConfig);
        when(mockAgentConfig.getAgentControlIntegrationConfig()).thenReturn(mockAgentControlIntegrationConfig);
    }

    @Test
    public void constructor_createsAgentHealth_withHealthyStatus() throws Exception {
        when(mockAgentControlIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockAgentControlIntegrationConfig.isEnabled()).thenReturn(true);
        AgentControlHealthUnitTestClient healthClient = new AgentControlHealthUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, mockAgentConfig);
        service.doStart();
        Thread.sleep(2100);

        assertTrue(healthClient.getAgentHealth().isHealthy());
        assertEquals("Healthy", healthClient.getAgentHealth().getCurrentStatus());
        service.doStop();
    }

    @Test
    public void onUnhealthyStatus_updatesAgentHealthToUnhealthy() throws Exception {
        when(mockAgentControlIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockAgentControlIntegrationConfig.isEnabled()).thenReturn(true);
        AgentControlHealthUnitTestClient healthClient = new AgentControlHealthUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, mockAgentConfig);
        service.onUnhealthyStatus(AgentHealth.Status.GC_CIRCUIT_BREAKER, "1", "2");
        service.doStart();
        Thread.sleep(2100);

        assertFalse(healthClient.getAgentHealth().isHealthy());
        assertEquals("Garbage collection circuit breaker triggered: Percent free memory 1; GC CPU time: 2", healthClient.getAgentHealth().getCurrentStatus());

        service.doStop();
    }

    @Test
    public void onHealthyStatus_updatesAgentHealthToHealthy() throws Exception {
        when(mockAgentControlIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockAgentControlIntegrationConfig.isEnabled()).thenReturn(true);
        AgentControlHealthUnitTestClient healthClient = new AgentControlHealthUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, mockAgentConfig);
        service.onUnhealthyStatus(AgentHealth.Status.GC_CIRCUIT_BREAKER, "1", "2");
        service.doStart();
        Thread.sleep(2100);

        assertFalse(healthClient.getAgentHealth().isHealthy());
        assertEquals("Garbage collection circuit breaker triggered: Percent free memory 1; GC CPU time: 2", healthClient.getAgentHealth().getCurrentStatus());

        Thread.sleep(1100);

        service.onHealthyStatus(AgentHealth.Category.CIRCUIT_BREAKER);
        assertTrue(healthClient.getAgentHealth().isHealthy());
        assertEquals("Healthy", healthClient.getAgentHealth().getCurrentStatus());

        service.doStop();
    }

    @Test
    public void doStop_writesShutdownHealthStatus() throws Exception {
        when(mockAgentControlIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockAgentControlIntegrationConfig.isEnabled()).thenReturn(true);
        AgentControlHealthUnitTestClient healthClient = new AgentControlHealthUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, mockAgentConfig);
        service.doStart();
        service.doStop();

        assertFalse(healthClient.getAgentHealth().isHealthy());
        assertEquals("NR-APM-099", healthClient.getAgentHealth().getLastError());
        assertEquals("Agent has shutdown", healthClient.getAgentHealth().getCurrentStatus());
    }


    @Test
    public void doStart_ignoresStartCommand_whenEnabledIsFalse() throws Exception {
        when(mockAgentControlIntegrationConfig.isEnabled()).thenReturn(false);
        AgentControlHealthUnitTestClient healthClient = new AgentControlHealthUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, mockAgentConfig);
        service.doStart();
        Thread.sleep(2100);

        assertNull(healthClient.getAgentHealth());
    }
}
