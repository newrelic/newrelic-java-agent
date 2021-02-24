package com.newrelic.agent.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.json.simple.JSONValue;
import org.junit.Test;

public class AgentArgumentsTest {

    @Test
    public void serialize() {
        AgentArguments args = new AgentArguments(new HashMap<String, String>(), new HashMap<String, String>());
        String jsonString = JSONValue.toJSONString(args.update(null, null, 123, "111"));
        AgentArguments deserialize = AgentArguments.fromJsonObject(JSONValue.parse(jsonString));
        assertEquals(123, deserialize.getServerPort().intValue());
        assertEquals("111", deserialize.getId());
    }
}
