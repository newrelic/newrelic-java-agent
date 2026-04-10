/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.agentcontrol.effectiveconfig.AgentControlIntegrationEffectiveConfigClient;
import com.newrelic.agent.agentcontrol.health.AgentHealth;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfig;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
    Map<String, Object> mockEffectiveConfig;

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

        mockEffectiveConfig = new HashMap<>();
        mockEffectiveConfig.put("log_level", "FINEST");
        mockEffectiveConfig.put("audit_mode", true);
        mockEffectiveConfig.put("jar_collector.enabled", true);
        mockEffectiveConfig.put("jfr.enabled", true);
        mockEffectiveConfig.put("jfr.harvest_interval", 10);
        mockEffectiveConfig.put("jfr.queue_size", 20000);
    }

    @Test
    public void constructor_createsAgentHealth_withHealthyStatus() throws Exception {
        when(mockAgentControlIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockAgentControlIntegrationConfig.isEnabled()).thenReturn(true);
        AgentControlHealthUnitTestClient healthClient = new AgentControlHealthUnitTestClient();
        AgentControlEffectiveConfigUnitTestClient effectiveConfigClient = new AgentControlEffectiveConfigUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, effectiveConfigClient, mockAgentConfig);
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
        AgentControlEffectiveConfigUnitTestClient effectiveConfigClient = new AgentControlEffectiveConfigUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, effectiveConfigClient, mockAgentConfig);
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
        AgentControlEffectiveConfigUnitTestClient effectiveConfigClient = new AgentControlEffectiveConfigUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, effectiveConfigClient, mockAgentConfig);
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
        AgentControlEffectiveConfigUnitTestClient effectiveConfigClient = new AgentControlEffectiveConfigUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, effectiveConfigClient, mockAgentConfig);
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
        AgentControlEffectiveConfigUnitTestClient effectiveConfigClient = new AgentControlEffectiveConfigUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, effectiveConfigClient, mockAgentConfig);
        service.doStart();
        Thread.sleep(2100);

        assertNull(healthClient.getAgentHealth());
        assertNull(effectiveConfigClient.effectiveConfig);
    }

    @Test
    public void constructor_writesEffectiveConfig() throws Exception {
        when(mockAgentControlIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockAgentControlIntegrationConfig.isEnabled()).thenReturn(true);
        AgentControlHealthUnitTestClient healthClient = new AgentControlHealthUnitTestClient();
        AgentControlEffectiveConfigUnitTestClient effectiveConfigClient = new AgentControlEffectiveConfigUnitTestClient();
        AgentControlIntegrationService service = new AgentControlIntegrationService(healthClient, effectiveConfigClient, mockAgentConfig);
        service.doStart();
        Thread.sleep(2100);

        assertTrue(healthClient.getAgentHealth().isHealthy());
        assertEquals("Healthy", healthClient.getAgentHealth().getCurrentStatus());
        service.doStop();
    }
}
