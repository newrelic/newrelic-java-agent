package com.newrelic.agent.transport.serverless;

public class DataSenderServerlessConfig {
    private final String agentVersion;

    public DataSenderServerlessConfig(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getAgentVersion() {
        return agentVersion;
    }
}
