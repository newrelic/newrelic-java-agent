package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.config.ServerlessConfig;

public class DataSenderServerlessConfig {
    private final String agentVersion;
    private final ServerlessConfig serverlessConfig;

    public DataSenderServerlessConfig(String agentVersion, ServerlessConfig serverlessConfig) {
        this.agentVersion = agentVersion;
        this.serverlessConfig = serverlessConfig;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public ServerlessConfig getServerlessConfig() {
        return serverlessConfig;
    }
}
