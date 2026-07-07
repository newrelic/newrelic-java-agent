package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.logging.IAgentLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.logging.Level;

public class ServerlessWriterImpl implements ServerlessWriter {
    private final IAgentLogger logger;
    private File pathFile;
    private boolean fileCreated;

    public ServerlessWriterImpl(IAgentLogger logger, String filePath) {
        this.logger = logger;
        try {
            pathFile = new File(filePath);
            fileCreated = true;
        } catch (Throwable t) {
            fileCreated = false;
            pathFile = null;
        }

    }

    @Override
    public synchronized void write(String filePayload, String consolePayload) {
        boolean fileWritten = false;
        if (fileCreated && pathFile.exists()) {
            try (BufferedWriter pipe = new BufferedWriter(new FileWriter(pathFile, false))) {
                pipe.write(filePayload);
                fileWritten = true;
            } catch (Throwable ex) {
                logger.log(Level.FINEST, "Failed to write payload, writing to console instead", ex);
            }
        }
        if (!fileWritten) {
            System.out.println(consolePayload);
        }
    }
}