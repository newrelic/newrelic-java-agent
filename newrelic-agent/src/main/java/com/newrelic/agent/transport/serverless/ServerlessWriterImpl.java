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
        logger.log(Level.FINEST, "Serverless Writer attempting to create a pipe file of path {0}", filePath);
        try {
            pathFile = new File(filePath);
            fileCreated = true;
        } catch (Throwable t) {
            fileCreated = false;
            pathFile = null;
            logger.log(Level.FINEST, "Failed to create pipe file " + filePath + " for Serverless Mode, writing to console instead", t);
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