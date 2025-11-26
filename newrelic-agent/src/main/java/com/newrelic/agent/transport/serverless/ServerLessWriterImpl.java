package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.logging.IAgentLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;

public class ServerlessWriterImpl implements ServerlessWriter {
    private static final String FILE_PATH = "/tmp/newrelic-telemetry";
    private final IAgentLogger logger;
    private final File pathFile;

    public ServerlessWriterImpl(IAgentLogger logger) {
        this.logger = logger;
        pathFile = new File(FILE_PATH);
    }

    @Override
    public void write(String filePayload, String consolePayload) {
        if (pathFile.exists()) {
            try (BufferedWriter pipe = new BufferedWriter(new FileWriter(pathFile, false))) {
                pipe.write(filePayload);
            } catch (IOException ignored) {
                logger.log(Level.FINEST, "Failed to write payload", ignored);
            }
        }
        System.out.println(consolePayload);
    }
}