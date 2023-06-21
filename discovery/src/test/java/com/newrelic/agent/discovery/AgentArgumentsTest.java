package com.newrelic.agent.discovery;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class AgentArgumentsTest {

    @Test
    public void serialize() {
        AgentArguments args = new AgentArguments(new HashMap<>(), new HashMap<>());
        String jsonString = JSONValue.toJSONString(args.update(null, null, 123, "111"));
        AgentArguments deserialize = AgentArguments.fromJsonObject(JSONValue.parse(jsonString));
        assertEquals(123, deserialize.getServerPort().intValue());
        assertEquals("111", deserialize.getId());
    }

    @Test
    public void update_createsEnvMapAndSysPropMap() {
        AgentArguments args = new AgentArguments(new HashMap<>(), new HashMap<>());
        args = args.update("my-app", "cmd-line", 9999, "9876");

        assertEquals("my-app", args.getEnvironment().get("NEW_RELIC_APP_NAME"));
        assertEquals("cmd-line", args.getEnvironment().get("NEW_RELIC_COMMAND_LINE"));
        assertEquals("9876", args.getId());
        assertEquals(9999, args.getServerPort());
    }

    @Test
    public void toJsonString_producesCorrectJson() throws ParseException {
        AgentArguments args = new AgentArguments(new HashMap<>(), new HashMap<>());
        args = args.update("my-app", "cmd-line", 123, "111");
        String jsonString = args.toJSONString();

        JSONObject json = (JSONObject) (new JSONParser().parse(jsonString));
        assertEquals("111", json.get("id"));
        assertEquals(123L, json.get("serverPort"));
        assertEquals(4, json.size());
    }

    @Test
    public void fromJsonObject_producesCorrectAgentArgObject() throws ParseException {
        String jsonString = "{\"environment\":{\"NEW_RELIC_COMMAND_LINE\":\"cmd-line\",\"NEW_RELIC_APP_NAME\":\"my-app\"},\"id\":\"9876\",\"serverPort\":123,\"properties\":{}}";
        JSONObject json = (JSONObject) (new JSONParser().parse(jsonString));
        AgentArguments args = AgentArguments.fromJsonObject(json);

        assertEquals("my-app", args.getEnvironment().get("NEW_RELIC_APP_NAME"));
        assertEquals("cmd-line", args.getEnvironment().get("NEW_RELIC_COMMAND_LINE"));
        assertEquals("9876", args.getId());
        assertEquals(123L, args.getServerPort());
    }

    @Test
    public void testGetSystemProperties() {
        Map<String, String> systemPropsMap = new HashMap<>();
        systemPropsMap.put("system-property-A", "system-property-value-A");
        AgentArguments agentArguments = new AgentArguments(new HashMap<>(), systemPropsMap);

        assertEquals(systemPropsMap, agentArguments.getSystemProperties());

    }
}
