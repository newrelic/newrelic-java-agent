/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.config.SuperAgentIntegrationConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuperAgentIntegrationHealthFileBasedClientTest {
    private static URI HEALTH_FILE_LOCATION = null;

    static {
        try {
            HEALTH_FILE_LOCATION = new URI("file://" + System.getProperty("user.dir"));
        } catch (URISyntaxException e) {
            // ignored
        }
    }

    private SuperAgentIntegrationConfig mockConfig;

    @Before
    public void setup() {
        mockConfig = mock(SuperAgentIntegrationConfig.class);
    }

    @After
    public void cleanup() {
        File[] healthFiles = getGeneratedHealthFiles();
        if (healthFiles != null) {
            for (File f : healthFiles) {
                f.delete();
            }
        }
    }

    @Test
    public void sendHealthMessage_withValidConfig_createsHealthFile() throws IOException {
        when(mockConfig.getHealthDeliveryLocation()).thenReturn(HEALTH_FILE_LOCATION);
        SuperAgentIntegrationHealthFileBasedClient client = new SuperAgentIntegrationHealthFileBasedClient(mockConfig);

        long startTime = SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos();
        AgentHealth agentHealth = new AgentHealth(startTime);

        client.sendHealthMessage(agentHealth);
        File[] yamlFiles = getGeneratedHealthFiles();
        Yaml yaml = new Yaml();
        InputStream is = Files.newInputStream(yamlFiles[0].toPath());
        Map<String, Object> parsedYaml = yaml.load(is);
        assertTrue((boolean)parsedYaml.get("healthy"));
        assertEquals(startTime, parsedYaml.get("start_time_unix_nano"));
        assertEquals("Healthy", parsedYaml.get("status"));
        assertNotNull(parsedYaml.get("status_time_unix_nano"));
        assertNull(parsedYaml.get("last_error"));

        assertTrue(client.isValid());
    }

    @Test
    public void sendHealthMessage_withUnhealthyAgentInstance_createsHealthFileWithLastError() throws IOException {
        when(mockConfig.getHealthDeliveryLocation()).thenReturn(HEALTH_FILE_LOCATION);
        SuperAgentIntegrationHealthFileBasedClient client = new SuperAgentIntegrationHealthFileBasedClient(mockConfig);

        long startTime = SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos();
        AgentHealth agentHealth = new AgentHealth(startTime);
        agentHealth.setUnhealthyStatus(AgentHealth.Status.INVALID_LICENSE);

        client.sendHealthMessage(agentHealth);
        File[] yamlFiles = getGeneratedHealthFiles();
        Yaml yaml = new Yaml();
        InputStream is = Files.newInputStream(yamlFiles[0].toPath());
        Map<String, Object> parsedYaml = yaml.load(is);
        assertFalse((boolean)parsedYaml.get("healthy"));
        assertEquals(startTime, parsedYaml.get("start_time_unix_nano"));
        assertEquals("Invalid license key (HTTP status code 401)", parsedYaml.get("status"));
        assertNotNull(parsedYaml.get("status_time_unix_nano"));
        assertEquals("NR-APM-001", parsedYaml.get("last_error"));

        assertTrue(client.isValid());
    }

    @Test
    public void constructor_withInvalidLocation_setsValidToFalse() throws URISyntaxException {
        when(mockConfig.getHealthDeliveryLocation()).thenReturn(new URI("file:///foo/bar/zzzzzzzz"));
        SuperAgentIntegrationHealthFileBasedClient client = new SuperAgentIntegrationHealthFileBasedClient(mockConfig);

        assertFalse(client.isValid());
    }

    private File[] getGeneratedHealthFiles() {
        File yamlFile = new File(HEALTH_FILE_LOCATION);
        File[] files = yamlFile.listFiles((dir, name) -> name.matches( "health-.*\\.yml" ));
        return (files != null && files.length > 0) ? files : null;
    }
}
