/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthClient;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthFileBasedClient;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthNoOpClient;
import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfig;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentControlIntegrationClientFactoryTest {
    private final String URI_TEST_STRING = "file://" + System.getProperty("user.dir") + "/health.yml";
    private AgentControlIntegrationConfig mockConfig;
    private IRPMService mockRpmService;

    @Before
    public void setup() {
        mockConfig = mock(AgentControlIntegrationConfig.class);
        mockRpmService = mock(RPMService.class);
    }

    @Test
    public void createHealthClient_withInvalidType_returnsNoOpClient() {
        AgentControlIntegrationHealthClient client = AgentControlIntegrationClientFactory.createHealthClient(mockConfig);
        assertTrue(client instanceof AgentControlIntegrationHealthNoOpClient);
    }

    @Test
    public void createHealthClient_withFileType_returnsFileBasedClient() throws URISyntaxException {
        URI uri = new URI(URI_TEST_STRING);
        when(mockConfig.getHealthDeliveryLocation()).thenReturn(uri);
        when(mockConfig.getHealthClientType()).thenReturn("file");
        AgentControlIntegrationHealthClient client = AgentControlIntegrationClientFactory.createHealthClient(mockConfig);
        assertTrue(client instanceof AgentControlIntegrationHealthFileBasedClient);
    }

    @Test
    public void createHealthClient_withNoOpType_returnsNoOpClient() {
        when(mockConfig.getHealthClientType()).thenReturn("noop");
        AgentControlIntegrationHealthClient client = AgentControlIntegrationClientFactory.createHealthClient(mockConfig);
        assertTrue(client instanceof AgentControlIntegrationHealthNoOpClient);
    }
}
