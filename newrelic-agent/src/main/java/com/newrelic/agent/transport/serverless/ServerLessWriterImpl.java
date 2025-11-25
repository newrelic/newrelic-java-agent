package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.logging.IAgentLogger;
import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;

public class ServerLessWriterImpl implements ServerlessWriter {
    private static final String FILE_PATH = "/tmp/newrelic-telemetry";
    private final IAgentLogger logger;
    private final File pathFile;

    public ServerLessWriterImpl(IAgentLogger logger) {
        this.logger = logger;
        pathFile = new File(FILE_PATH);
    }

    @Override
    public void write(JSONObject payload) {
        String payloadString = payload.toJSONString();
        if (pathFile.exists()) {
            try (BufferedWriter pipe = new BufferedWriter(new FileWriter(pathFile, false))) {
                pipe.write(payloadString);
            } catch (IOException ignored) {
                logger.log(Level.FINEST, "Failed to write payload", ignored);
            }
        }
        System.out.println(payloadString);
    }
}
