package com.newrelic.agent.discovery;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.Test;

public class StatusServerTest {
    @Test
    public void startServerAndSendMessage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonAttachOutput output = JsonAttachOutputTest.createJsonAttachOutput(out);
        final String id = "5";
        output.attachStarted(id, "test.jar", "");
        try (StatusServer server = StatusServer.createAndStart(output)) {
            StatusClient client = StatusClient.create(server.getPort());
            client.write(StatusMessage.info(id, "Info", "Test"));
            server.flush();
        }
        output.attachFinished();
        output.close();

        assertEquals("[{\"agentArgs\":\"\",\"success\":false,\"messages\":[{\"level\":\"INFO\",\"messsage\":\"Test\",\"label\":\"Info\"}],\"pid\":5,\"command\":\"test.jar\"}]\n",
                out.toString());
    }

    @Test
    public void startServerAndDiscover() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonAttachOutput output = JsonAttachOutputTest.createJsonAttachOutput(out);
        output.list("1", "test", "1.8", true);
        try (StatusServer server = StatusServer.createAndStart(output)) {
            StatusClient client = StatusClient.create(server.getPort());
            client.write(new ApplicationContainerInfo("1", "Jetty", Arrays.asList("Name")));
            server.flush();
        }
        output.close();

        assertEquals("[{\"displayName\":\"test\",\"containerName\":\"Jetty\",\"vmVersion\":\"1.8\",\"pid\":1,\"applicationNames\":[\"Name\"],\"attachable\":true}]\n",
                out.toString());
    }
}
