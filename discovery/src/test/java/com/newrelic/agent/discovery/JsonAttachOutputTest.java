package com.newrelic.agent.discovery;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.Test;

public class JsonAttachOutputTest {

    @Test
    public void simpleTest() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonAttachOutput output = createJsonAttachOutput(out);
        output.attachStarted("1", "test.jar", "{}");
        output.attachFinished();
        output.close();

        assertEquals("[{\"agentArgs\":\"{}\",\"success\":false,\"messages\":[],\"pid\":1,\"command\":\"test.jar\"}]\n",
                out.toString());
    }

    @Test
    public void messageTest() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonAttachOutput output = createJsonAttachOutput(out);
        output.attachStarted("1", "test.jar", "{}");
        output.write(StatusMessage.info("1", "Info", "Test"));
        output.attachFinished();
        output.close();

        assertEquals("[{\"agentArgs\":\"{}\",\"success\":false,\"messages\":[{\"level\":\"INFO\",\"messsage\":\"Test\",\"label\":\"Info\"}],\"pid\":1,\"command\":\"test.jar\"}]\n",
                out.toString());
    }

    @Test
    public void successTest() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonAttachOutput output = createJsonAttachOutput(out);
        output.attachStarted("1", "test.jar", "{}");
        output.write(StatusMessage.success("1", "http://localhost"));
        output.attachFinished();
        output.close();

        assertEquals("[{\"agentArgs\":\"{}\",\"success\":true,\"messages\":[{\"level\":\"INFO\",\"messsage\":\"http:\\/\\/localhost\",\"label\":\"Url\"}],\"pid\":1,\"command\":\"test.jar\"}]\n",
                out.toString());
    }

    @Test
    public void processTest() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonAttachOutput output = createJsonAttachOutput(out);
        output.list("6", "test.jar", "1.8", true);
        output.close();
        assertEquals("[{\"displayName\":\"test.jar\",\"vmVersion\":\"1.8\",\"pid\":6,\"attachable\":true}]\n",
                out.toString());
    }

    static JsonAttachOutput createJsonAttachOutput(OutputStream output) {
        PrintStream printStream = new PrintStream(output);
        JsonSerializer serializer = new DefaultJsonSerializer();
        return new JsonAttachOutput(printStream, serializer);
    }
}
