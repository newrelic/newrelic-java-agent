/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigFileHelper;

import java.io.File;
import java.text.MessageFormat;

/**
 * Utility class to get the New Relic log file.
 */
class LogFileHelper {

    private static final String NEW_RELIC_LOG_FILE = "newrelic.logfile";
    private static final String LOGS_DIRECTORY = "logs";

    /**
     * Get the New Relic log file.
     *
     * @return the New Relic log file or null
     */
    public static File getLogFile(AgentConfig agentConfig) {
        if (agentConfig.isLoggingToStdOut()) {
            return null;
        }
        File f = getLogFileFromProperty();
        if (f != null) {
            return f;
        }
        return getLogFileFromConfig(agentConfig);
    }

    private static File getLogFileFromProperty() {
        String logFileName = System.getProperty(NEW_RELIC_LOG_FILE);
        if (logFileName == null) {
            return null;
        }
        File f = new File(logFileName);
        try {
            f.createNewFile();
            return f;
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to create log file {0}. Check permissions on the directory. - {1}", logFileName, e);
            Agent.LOG.warning(msg);
        }
        return null;
    }

    private static File getLogFileFromConfig(AgentConfig agentConfig) {
        String logFileName = agentConfig.getLogFileName();
        File logsDirectory = getLogsDirectory(agentConfig);
        return new File(logsDirectory, logFileName);
    }

    /**
     * Get the logs directory.
     */
    private static File getLogsDirectory(AgentConfig agentConfig) {
        File f = getLogsDirectoryFromConfig(agentConfig);
        if (f != null) {
            return f;
        }

        f = getNewRelicLogsDirectory();
        if (f != null) {
            return f;
        }

        f = new File(LOGS_DIRECTORY);
        if (f.exists()) {
            return f;
        }

        return new File(".");
    }

    /**
     * Get the log directory from the agent configuration.
     */
    private static File getLogsDirectoryFromConfig(AgentConfig agentConfig) {
        String logFilePath = agentConfig.getLogFilePath();
        if (logFilePath == null) {
            return null;
        }
        File f = new File(logFilePath);
        if (f.exists()) {
            return f;
        } else {
            String msg = MessageFormat.format("The log_file_path {0} specified in newrelic.yml does not exist", logFilePath);
            Agent.LOG.config(msg);
        }
        return null;
    }

    /**
     * Get the logs directory in the New Relic directory.
     */
    private static File getNewRelicLogsDirectory() {
        File nrDir = ConfigFileHelper.getNewRelicDirectory();
        if (nrDir != null) {
            File logs = new File(nrDir, LOGS_DIRECTORY);
            logs.mkdir();
            return logs;
        }
        return null;
    }

}
