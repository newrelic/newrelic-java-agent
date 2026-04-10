package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.agentcontrol.effectiveconfig.AgentControlIntegrationEffectiveConfigFileBasedClient;
import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfig;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentControlIntegrationEffectiveConfigFileBasedClientTest {
    private static URI EFFECTIVE_CONFIG_FILE_LOCATION = null;

    static {
        try {
            EFFECTIVE_CONFIG_FILE_LOCATION = new URI("file://" + System.getProperty("user.dir"));
        } catch (URISyntaxException ignored) {
        }
    }

    private AgentControlIntegrationConfig mockConfig;

    @Before
    public void setup() {
        mockConfig = mock(AgentControlIntegrationConfig.class);
    }

    @After
    public void cleanup() {
        File[] effectiveConfigFiles = getGeneratedEffectiveConfigFiles();
        if (effectiveConfigFiles != null) {
            for (File f : effectiveConfigFiles) {
                f.delete();
            }
        }
    }

    @Test
    public void sendEffectiveConfig_withValidConfig_createsEffectiveConfigFile() throws IOException {
        when(mockConfig.getEffectiveConfigDeliveryLocation()).thenReturn(EFFECTIVE_CONFIG_FILE_LOCATION);
        AgentControlIntegrationEffectiveConfigFileBasedClient client = new AgentControlIntegrationEffectiveConfigFileBasedClient(mockConfig);
        Map<String, Object> mockConfig = new HashMap<>();
        mockConfig.put("foo", "bar");
        mockConfig.put("boo", "baz");
        mockConfig.put("zoo", "zat");
        mockConfig.put("bool", true);
        mockConfig.put("int", 123);

        client.sendEffectiveConfigMessage(mockConfig);

        File[] yamlFiles = getGeneratedEffectiveConfigFiles();
        Yaml yaml = new Yaml();
        InputStream is = Files.newInputStream(yamlFiles[0].toPath());
        Map<String, Object> parsedYaml = yaml.load(is);
        assertTrue((boolean)parsedYaml.get("bool"));
        assertEquals(123, parsedYaml.get("int"));
        assertEquals("bar", parsedYaml.get("foo"));
        assertEquals("baz", parsedYaml.get("boo"));
        assertEquals("zat", parsedYaml.get("zoo"));

        assertTrue(client.isValid());
    }

    @Test
    public void constructor_withInvalidLocation_setsValidToFalse() throws URISyntaxException {
        when(mockConfig.getEffectiveConfigDeliveryLocation()).thenReturn(new URI("file:///foo/bar/zzzzzzzz"));
        AgentControlIntegrationEffectiveConfigFileBasedClient client =
                new AgentControlIntegrationEffectiveConfigFileBasedClient(mockConfig);

        assertFalse(client.isValid());
    }

    private File[] getGeneratedEffectiveConfigFiles() {
        File yamlFile = new File(EFFECTIVE_CONFIG_FILE_LOCATION);
        File[] files = yamlFile.listFiles((dir, name) -> name.matches( "effective_config-.*\\.yml" ));
        return (files != null && files.length > 0) ? files : null;
    }
}
