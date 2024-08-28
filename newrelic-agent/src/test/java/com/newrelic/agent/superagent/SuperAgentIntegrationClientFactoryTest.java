/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.config.SuperAgentIntegrationConfig;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuperAgentIntegrationClientFactoryTest {
    private final String URI_TEST_STRING = "file://" + System.getProperty("user.dir") + "/health.yml";
    private SuperAgentIntegrationConfig mockConfig;

    @Before
    public void setup() {
        mockConfig = mock(SuperAgentIntegrationConfig.class);
    }

    @Test
    public void createHealthClient_withInvalidType_returnsNoOpClient() {
        SuperAgentIntegrationHealthClient client = SuperAgentIntegrationClientFactory.createHealthClient("foo", mockConfig);
        assertTrue(client instanceof SuperAgentHealthNoOpClient);
    }

    @Test
    public void createHealthClient_withFileType_returnsFileBasedClient() throws URISyntaxException {
        URI uri = new URI(URI_TEST_STRING);
        when(mockConfig.getHealthDeliveryLocation()).thenReturn(uri);
        SuperAgentIntegrationHealthClient client = SuperAgentIntegrationClientFactory.createHealthClient("file", mockConfig);
        assertTrue(client instanceof SuperAgentIntegrationHealthFileBasedClient);
    }

    @Test
    public void createHealthClient_withNoOpType_returnsNoOpClient() {
        SuperAgentIntegrationHealthClient client = SuperAgentIntegrationClientFactory.createHealthClient("noop", mockConfig);
        assertTrue(client instanceof SuperAgentHealthNoOpClient);
    }

}
