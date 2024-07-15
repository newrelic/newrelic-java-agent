package com.newrelic.agent.superagent;

import java.io.File;

public class SuperAgentIntegrationFileBasedClient implements SuperAgentIntegrationClient {
    private File healthFile = null;

    public SuperAgentIntegrationFileBasedClient(String healthFile) {
        if (healthFile != null) {
            this.healthFile = new File(healthFile);
        }
    }

    @Override
    public void sendHealthMessage(String message) {

    }
}
