package com.newrelic.agent.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.json.simple.JSONValue;
import org.junit.Test;
import org.mockito.Mockito;

public class AgentArgumentsTest {

    @Test
    public void serialize() {
        AgentArguments args = new AgentArguments(new HashMap<String, String>(), new HashMap<String, String>());
        String jsonString = JSONValue.toJSONString(args.update(null, null, 123, "111"));
        AgentArguments deserialize = AgentArguments.fromJsonObject(JSONValue.parse(jsonString));
        assertEquals(123, deserialize.getServerPort().intValue());
        assertEquals("111", deserialize.getId());
    }

    @Test
    public void fromOptions() {
        AttachOptions options = Mockito.mock(AttachOptions.class);
        Mockito.when(options.getLicenseKey()).thenReturn("license");
        AgentArguments args = AgentArguments.getAgentArguments(options);
        assertNotNull(args.getEnvironment());
        assertNotNull(args.getSystemProperties());
    }

    @Test
    public void discovery() {
        AgentArguments args = AgentArguments.getDiscoveryAgentArguments().update(null, null, 123, "1");
        assertNotNull(args.getEnvironment());
        assertNotNull(args.getSystemProperties());
        assertEquals(123, args.getServerPort());
        assertEquals("1", args.getId());
        assertTrue(args.isDiscover());
    }

    @Test
    public void noLicense() {
        AttachOptions options = Mockito.mock(AttachOptions.class);
        try {
            AgentArguments.getAgentArguments(options);
        } catch (IllegalArgumentException e) {
            assertEquals("Please specify the account license key with -license", e.getMessage());
        }
    }
}
