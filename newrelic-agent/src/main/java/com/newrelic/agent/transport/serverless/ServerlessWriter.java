package com.newrelic.agent.transport.serverless;

import org.json.simple.JSONObject;

public interface ServerlessWriter {
    void write(String filePayload, String consolePayload);
}
