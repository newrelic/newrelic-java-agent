/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.config.AgentControlIntegrationConfig;
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

    @Before
    public void setup() {
        mockConfig = mock(AgentControlIntegrationConfig.class);
    }

    @Test
    public void createHealthClient_withInvalidType_returnsNoOpClient() {
        AgentControlIntegrationHealthClient client = AgentControlIntegrationClientFactory.createHealthClient(mockConfig);
        assertTrue(client instanceof AgentControlHealthNoOpClientControl);
    }

    @Test
    public void createHealthClient_withFileType_returnsFileBasedClient() throws URISyntaxException {
        URI uri = new URI(URI_TEST_STRING);
        when(mockConfig.getHealthDeliveryLocation()).thenReturn(uri);
        when(mockConfig.getHealthClientType()).thenReturn("file");
        AgentControlIntegrationHealthClient client = AgentControlIntegrationClientFactory.createHealthClient(mockConfig);
        assertTrue(client instanceof AgentControlControlIntegrationHealthFileBasedClient);
    }

    @Test
    public void createHealthClient_withNoOpType_returnsNoOpClient() {
        when(mockConfig.getHealthClientType()).thenReturn("noop");
        AgentControlIntegrationHealthClient client = AgentControlIntegrationClientFactory.createHealthClient(mockConfig);
        assertTrue(client instanceof AgentControlHealthNoOpClientControl);
    }
}
