package com.newrelic.agent.superagent;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.SuperAgentIntegrationConfig;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuperAgentIntegrationServiceTest {
    AgentConfig mockAgentConfig;
    SuperAgentIntegrationConfig mockSuperAgentIntegrationConfig;
    AgentHealth mockAgentHealth;

    @Before
    public void before() {
        mockAgentConfig = mock(AgentConfig.class);
        mockSuperAgentIntegrationConfig = mock(SuperAgentIntegrationConfig.class);
        mockAgentHealth = mock(AgentHealth.class);

        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);

        when(mockAgentConfig.getSuperAgentIntegrationConfig()).thenReturn(mockSuperAgentIntegrationConfig);

    }

    @Test
    public void constructor_createsAgentHealth_withHealthyStatus() throws Exception {
        when(mockSuperAgentIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockSuperAgentIntegrationConfig.isEnabled()).thenReturn(true);
        SuperAgentHealthUnitTestClient healthClient = new SuperAgentHealthUnitTestClient();
        SuperAgentIntegrationService service = new SuperAgentIntegrationService(healthClient, mockAgentConfig);
        service.doStart();
        Thread.sleep(2100);

        assertTrue(healthClient.getAgentHealth().isHealthy());
        assertEquals("Healthy", healthClient.getAgentHealth().getCurrentStatus());
        service.doStop();
    }

    @Test
    public void onUnhealthyStatus_updatesAgentHealthToUnhealthy() throws Exception {
        when(mockSuperAgentIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockSuperAgentIntegrationConfig.isEnabled()).thenReturn(true);
        SuperAgentHealthUnitTestClient healthClient = new SuperAgentHealthUnitTestClient();
        SuperAgentIntegrationService service = new SuperAgentIntegrationService(healthClient, mockAgentConfig);
        service.onUnhealthyStatus(AgentHealth.Status.GC_CIRCUIT_BREAKER, "1", "2");
        service.doStart();
        Thread.sleep(2100);

        assertFalse(healthClient.getAgentHealth().isHealthy());
        assertEquals("Garbage collection circuit breaker triggered: Percent free memory 1; GC CPU time: 2", healthClient.getAgentHealth().getCurrentStatus());

        service.doStop();
    }

    @Test
    public void onHealthyStatus_updatesAgentHealthToHealthy() throws Exception {
        when(mockSuperAgentIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockSuperAgentIntegrationConfig.isEnabled()).thenReturn(true);
        SuperAgentHealthUnitTestClient healthClient = new SuperAgentHealthUnitTestClient();
        SuperAgentIntegrationService service = new SuperAgentIntegrationService(healthClient, mockAgentConfig);
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
        when(mockSuperAgentIntegrationConfig.getHealthReportingFrequency()).thenReturn(1);
        when(mockSuperAgentIntegrationConfig.isEnabled()).thenReturn(true);
        SuperAgentHealthUnitTestClient healthClient = new SuperAgentHealthUnitTestClient();
        SuperAgentIntegrationService service = new SuperAgentIntegrationService(healthClient, mockAgentConfig);
        service.doStart();
        service.doStop();

        assertFalse(healthClient.getAgentHealth().isHealthy());
        assertEquals("NR-APM-1000", healthClient.getAgentHealth().getLastError());
        assertEquals("Agent has shutdown", healthClient.getAgentHealth().getCurrentStatus());
    }


    @Test
    public void doStart_ignoresStartCommand_whenEnabledIsFalse() throws Exception {
        when(mockSuperAgentIntegrationConfig.isEnabled()).thenReturn(false);
        SuperAgentHealthUnitTestClient healthClient = new SuperAgentHealthUnitTestClient();
        SuperAgentIntegrationService service = new SuperAgentIntegrationService(healthClient, mockAgentConfig);
        service.doStart();
        Thread.sleep(2100);

        assertNull(healthClient.getAgentHealth());
    }

    //Simple health client that stores the last health message sent for unit test assertions
    private static class SuperAgentHealthUnitTestClient implements SuperAgentIntegrationHealthClient {

        private AgentHealth agentHealth;

        @Override
        public void sendHealthMessage(AgentHealth agentHealth) {
            this.agentHealth = agentHealth;
        }

        public AgentHealth getAgentHealth() {
            return agentHealth;
        }
    }
}
