package com.newrelic.agent.discovery;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

/**
 * The environment and system properties from the attach process are passed to the agent's
 * `agentmain` method as base 64 encoded json.
 */
public class AgentArguments implements JSONAware {
    public static final String NEW_RELIC_APP_NAME_ENV_VARIABLE = "NEW_RELIC_APP_NAME";
    public static final String NEW_RELIC_COMMAND_LINE_ENV_VARIABLE = "NEW_RELIC_COMMAND_LINE";

    private static final String SYSTEM_PROPERTIES_AGENT_ARGS_KEY = "properties";
    private static final String ENVIRONMENT_AGENT_ARGS_KEY = "environment";
    private static final String SERVER_PORT_AGENT_ARGS_KEY = "serverPort";
    private static final String ID_AGENT_ARGS_KEY = "id";

    private final Map<String, String> environment;
    private final Map<String, String> systemProperties;
    private Number serverPort;
    private String id;

    public AgentArguments(Map<String, String> environment, Map<String, String> systemProperties) {
        this.environment = environment;
        this.systemProperties = systemProperties;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public Number getServerPort() {
        return serverPort;
    }

    public String getId() {
        return id;
    }

    public void setAppName(String appName) {
        environment.put(NEW_RELIC_APP_NAME_ENV_VARIABLE, appName);
    }

    public void setCommandLine(String commandLine) {
        environment.put(NEW_RELIC_COMMAND_LINE_ENV_VARIABLE, commandLine);
    }

    public AgentArguments update(String appName, String commandLine, Integer serverPort, String pid) {
        final AgentArguments args = new AgentArguments(
                new HashMap<>(environment), new HashMap<>(systemProperties));
        if (appName != null) {
            args.setAppName(appName);
        }
        if (commandLine != null) {
            args.setCommandLine(commandLine);
        }
        args.serverPort = serverPort;
        args.id = pid;
        return args;
    }

    @SuppressWarnings("unchecked")
    public static AgentArguments fromJsonObject(Object object) {
        Map<String, Object> map = (Map<String, Object>) object;
        AgentArguments args = new AgentArguments(
                (Map<String, String>) map.get(ENVIRONMENT_AGENT_ARGS_KEY),
                (Map<String, String>) map.get(SYSTEM_PROPERTIES_AGENT_ARGS_KEY));
        args.serverPort = (Number) map.get(SERVER_PORT_AGENT_ARGS_KEY);
        args.id = (String) map.get(ID_AGENT_ARGS_KEY);
        return args;
    }

    @Override
    public String toJSONString() {
        Map<String, Object> args = new HashMap<>();
        args.put(ENVIRONMENT_AGENT_ARGS_KEY, environment);
        args.put(SYSTEM_PROPERTIES_AGENT_ARGS_KEY, systemProperties);
        args.put(ID_AGENT_ARGS_KEY, id);
        if (serverPort != null) {
            args.put(SERVER_PORT_AGENT_ARGS_KEY, serverPort);
        }
        return JSONObject.toJSONString(args);
    }
}
