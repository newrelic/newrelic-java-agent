package com.newrelic.agent.discovery;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

public class StatusServerTest {
    @Test
    public void startServerAndSendMessage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonAttachOutput output = JsonAttachOutputTest.createJsonAttachOutput(out);
        output.attachStarted("5", "test.jar", "");
        try (StatusServer server = StatusServer.createAndStart(output)) {
            StatusClient client = StatusClient.create(server.getPort());
            client.write(StatusMessage.info("1", "Info", "Test"));
            server.flush();
        }
        output.attachFinished();
        output.finished();

        assertEquals("[{\"agentArgs\":\"\",\"success\":false,\"messages\":[],\"pid\":5,\"command\":\"test.jar\"}]\n",
                out.toString());
    }
}
