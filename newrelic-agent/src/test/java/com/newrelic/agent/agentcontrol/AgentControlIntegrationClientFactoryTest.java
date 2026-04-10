/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.agentcontrol.effectiveconfig.AgentControlIntegrationEffectiveConfigClient;
import com.newrelic.agent.agentcontrol.effectiveconfig.AgentControlIntegrationEffectiveConfigFileBasedClient;
import com.newrelic.agent.agentcontrol.effectiveconfig.AgentControlIntegrationEffectiveConfigNoOpClient;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthClient;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthFileBasedClient;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthNoOpClient;
import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfig;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

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

    @Test
    public void createEffectiveConfigClient_withInvalidType_returnsNoOpClient() {
        AgentControlIntegrationEffectiveConfigClient client = AgentControlIntegrationClientFactory.createEffectiveConfigClient(mockConfig);
        assertTrue(client instanceof AgentControlIntegrationEffectiveConfigNoOpClient);
    }

    @Test
    public void createEffectiveConfigClient_withFileType_returnsFileBasedClient() throws URISyntaxException {
        URI uri = new URI(URI_TEST_STRING);
        when(mockConfig.getEffectiveConfigDeliveryLocation()).thenReturn(uri);
        when(mockConfig.getEffectiveConfigClientType()).thenReturn("file");
        AgentControlIntegrationEffectiveConfigClient client = AgentControlIntegrationClientFactory.createEffectiveConfigClient(mockConfig);
        assertTrue(client instanceof AgentControlIntegrationEffectiveConfigFileBasedClient);
    }

    @Test
    public void createEffectiveConfigClient_withNoOpType_returnsNoOpClient() {
        when(mockConfig.getEffectiveConfigClientType()).thenReturn("noop");
        AgentControlIntegrationEffectiveConfigClient client = AgentControlIntegrationClientFactory.createEffectiveConfigClient(mockConfig);
        assertTrue(client instanceof AgentControlIntegrationEffectiveConfigNoOpClient);
    }
}
