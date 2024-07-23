package com.newrelic.agent.superagent;

import com.newrelic.agent.config.SuperAgentIntegrationConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class SuperAgentIntegrationHealthFileBasedClientTest {
    private SuperAgentIntegrationConfig mockConfig;

    @Before
    public void setup() {
        mockConfig = mock(SuperAgentIntegrationConfig.class);
    }

    @After
    public void cleanup() {
        File healthFile = new File("./health.json");
        if (healthFile.exists()) {
            healthFile.delete();
        }
    }

    @Test
    public void sendHealthMessage_withValidConfig_createsHealthFile() throws IOException {
        when(mockConfig.getHealthDeliveryLocation()).thenReturn("./health.json");
        SuperAgentIntegrationHealthFileBasedClient client = new SuperAgentIntegrationHealthFileBasedClient(mockConfig);

        long startTime = SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos();
        AgentHealth agentHealth = new AgentHealth(startTime);

        client.sendHealthMessage(agentHealth);
        File yamlFile = new File("./health.json");
        Yaml yaml = new Yaml();
        InputStream is = Files.newInputStream(yamlFile.toPath());
        Map<String, Object> parsedYaml = yaml.load(is);
        assertTrue((boolean)parsedYaml.get("healthy"));
        assertEquals(startTime, parsedYaml.get("start_time_unix_nano"));
        assertEquals("Healthy", parsedYaml.get("status"));
        assertNotNull(parsedYaml.get("status_time_unix_nano"));
        assertNull(parsedYaml.get("last_error"));
    }

    @Test
    public void sendHealthMessage_withUnhealthyAgentInstance_createsHealthFileWithLastError() throws IOException {
        when(mockConfig.getHealthDeliveryLocation()).thenReturn("./health.json");
        SuperAgentIntegrationHealthFileBasedClient client = new SuperAgentIntegrationHealthFileBasedClient(mockConfig);

        long startTime = SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos();
        AgentHealth agentHealth = new AgentHealth(startTime);
        agentHealth.setUnhealthyStatus(AgentHealth.Status.INVALID_LICENSE);

        client.sendHealthMessage(agentHealth);
        File yamlFile = new File("./health.json");
        Yaml yaml = new Yaml();
        InputStream is = Files.newInputStream(yamlFile.toPath());
        Map<String, Object> parsedYaml = yaml.load(is);
        assertFalse((boolean)parsedYaml.get("healthy"));
        assertEquals(startTime, parsedYaml.get("start_time_unix_nano"));
        assertEquals("Invalid license key (HTTP status code 401)", parsedYaml.get("status"));
        assertNotNull(parsedYaml.get("status_time_unix_nano"));
        assertEquals("NR-APM-0010", parsedYaml.get("last_error"));
    }
}
