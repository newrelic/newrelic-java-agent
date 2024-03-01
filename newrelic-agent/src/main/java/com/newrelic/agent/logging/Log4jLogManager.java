/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import com.google.common.io.Resources;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AgentJarHelper;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

class Log4jLogManager implements IAgentLogManager {
    private static final String AGENT_JAR_LOG4J_CONFIG_FILE = "/META-INF/logging/log4j2.xml";

    /**
     * This needs to get set to keep errors from occurring.
     * See: https://logging.apache.org/log4j/2.0/manual/configuration.html#SystemProperties
     */
    private static final String CONFIG_FILE_PROP = "log4j2.configurationFile";
    private static final String LEGACY_CONFIG_FILE_PROP = "log4j.configurationFile";
    private static final String CONTEXT_SELECT_PROP = "log4j2.contextSelector";
    private static final String LEGACY_CONTEXT_SELECT_PROP = "Log4jContextSelector";
    private static final String CONTEXT_FACTORY_PROP = "log4j2.loggerContextFactory";
    private static final String DISABLE_JMX_PROP = "log4j2.disableJmx";
    private static final String LEGACY_DISABLE_JMX_PROP = "log4j2.disable.jmx";
    private static final String CLASSLOADER_PROP = "log4j2.ignoreTCL";
    private static final String LEGACY_CLASSLOADER_PROP = "log4j.ignoreTCL";
    private static final String JAVA_UTIL_LOG_MANAGER = "java.util.logging.manager";
    private static final String WS_LOG_MANAGER = "WsLogManager";

    private final Log4jLogger rootLogger;
    private volatile String logFilePath;

    private Log4jLogManager(String name) {
        rootLogger = initializeRootLogger(name);
    }

    private Log4jLogger createRootLogger(String name) {
        Log4jLogger logger = Log4jLogger.create(name, true);
        String logLevel = getStartupLogLevel();

        logger.setLevel(logLevel);
        logger.addConsoleAppender();
        return logger;
    }

    private String getStartupLogLevel() {
        String propName = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.STARTUP_LOG_LEVEL;
        String logLevel = System.getProperty(propName);
        if (logLevel == null) {
            return Level.INFO.toString().toLowerCase();
        } else {
            return logLevel.toLowerCase();
        }
    }

    private Log4jLogger initializeRootLogger(String name) {
        Log4jLogger logger = null;
        Map<String, String> systemProps = new HashMap<>();

        try {
            String jarFileName = AgentJarHelper.getAgentJarFileName();
            if (jarFileName == null) {
                logger = Log4jLogger.create(name, true);
            } else {
                // If they already set the config location, we want to clear it before configuring our logger.
                // and we want to clear any other log4j system properties
                clearAllLog4jSystemProperties(systemProps);

                URL log4jConfigXmlUrl = null;
                if (jarFileName.endsWith(".jar")) {
                    // it isn't enough to specify the jar, we have to specify the path within the jar
                    log4jConfigXmlUrl = new URL(new StringBuilder("jar:file:")
                            .append(jarFileName)
                            .append("!")
                            .append(AGENT_JAR_LOG4J_CONFIG_FILE)
                            .toString());
                } else {
                    // we likely have received a path to a set of class files (this happens when running tests)
                    try {
                        // guava's Resources class is usually smart enough to figure out where to find the log4j2.xml file
                        log4jConfigXmlUrl = Resources.getResource(this.getClass(), AGENT_JAR_LOG4J_CONFIG_FILE);
                    } catch (IllegalArgumentException iae) {
                        // fallback on path
                        log4jConfigXmlUrl = new File(jarFileName).toURI().toURL();
                    }
                }

                System.setProperty(CONFIG_FILE_PROP, log4jConfigXmlUrl.toString());
                System.setProperty(LEGACY_CONFIG_FILE_PROP, log4jConfigXmlUrl.toString());
                // Log4j won't be able to find log4j-provider.properties because it isn't on the classpath (it's in our agent) so this sets it manually
                System.setProperty(CONTEXT_FACTORY_PROP, "org.apache.logging.log4j.core.impl.Log4jContextFactory");

                // Prevent a JBoss failure that would occur here if the mbean server started up and used java.util.logger.LogManager
                System.setProperty(DISABLE_JMX_PROP, "true");
                System.setProperty(LEGACY_DISABLE_JMX_PROP, "true");

                // Log4j tries to get fancy with figuring out the classloader (and does it incorrectly) unless this flag is set.
                // I've no idea why, but using anything other than the legacy style property here can cause a segfault
                System.setProperty(LEGACY_CLASSLOADER_PROP, "true");
                System.setProperty(CLASSLOADER_PROP, "true");

                try {
                    logger = createRootLogger(name);
                } finally {
                    // Our logger is configured, restore back to original setting.
                    // be sure to remove our properties
                    System.getProperties().remove(CONFIG_FILE_PROP);
                    System.getProperties().remove(LEGACY_CONFIG_FILE_PROP);

                    System.getProperties().remove(CONTEXT_FACTORY_PROP);

                    System.getProperties().remove(DISABLE_JMX_PROP);
                    System.getProperties().remove(LEGACY_DISABLE_JMX_PROP);

                    System.getProperties().remove(CLASSLOADER_PROP);
                    System.getProperties().remove(LEGACY_CLASSLOADER_PROP);

                    applyOriginalSystemProperties(systemProps, logger);
                }
            }
        } catch (Exception e) {
            if (logger == null) {
                logger = createRootLogger(name);
            }
            String msg = MessageFormat.format("Error setting log4j.configurationFile property: {0}", e);
            logger.warning(msg);
        }
        return logger;
    }

    private void clearAllLog4jSystemProperties(Map<String, String> storedSystemProps) {
        // We've already set the logManager for Webshpere in BootstrapAgent and there's a bug that prevents us from
        // changing it here: https://www-01.ibm.com/support/docview.wss?uid=swg1PI74356
        String logManagerProp = System.getProperty(JAVA_UTIL_LOG_MANAGER);
        if (logManagerProp != null && !logManagerProp.contains(WS_LOG_MANAGER)) {
            clearLog4jSystemProperty(JAVA_UTIL_LOG_MANAGER, storedSystemProps);
        }

        clearLog4jSystemProperty(CONFIG_FILE_PROP, storedSystemProps);
        clearLog4jSystemProperty(LEGACY_CONFIG_FILE_PROP, storedSystemProps);

        clearLog4jSystemProperty(CONTEXT_SELECT_PROP, storedSystemProps);
        clearLog4jSystemProperty(LEGACY_CONTEXT_SELECT_PROP, storedSystemProps);

        clearLog4jSystemProperty(CONTEXT_FACTORY_PROP, storedSystemProps);

        clearLog4jSystemProperty(DISABLE_JMX_PROP, storedSystemProps);
        clearLog4jSystemProperty(LEGACY_DISABLE_JMX_PROP, storedSystemProps);

        clearLog4jSystemProperty(CLASSLOADER_PROP, storedSystemProps);
        clearLog4jSystemProperty(LEGACY_CLASSLOADER_PROP, storedSystemProps);
    }

    private void clearLog4jSystemProperty(String prop, Map<String, String> storedSystemProps) {
        String old = System.clearProperty(prop);
        if (old != null) {
            storedSystemProps.put(prop, old);
        }
    }

    private void applyOriginalSystemProperties(Map<String, String> storedSystemProps, Log4jLogger logger) {
        for (Entry<String, String> currentProp : storedSystemProps.entrySet()) {
            try {
                System.setProperty(currentProp.getKey(), currentProp.getValue());
            } catch (Exception e) {
                String msg = MessageFormat.format("Error setting log4j property {0} back to {1}. Error: {2}",
                        currentProp.getKey(), currentProp.getValue(), e);
                logger.warning(msg);
            }
        }
    }

    @Override
    public IAgentLogger getRootLogger() {
        return rootLogger;
    }

    @Override
    public String getLogFilePath() {
        return logFilePath;
    }

    @Override
    public void configureLogger(AgentConfig pAgentConfig) {
        configureLogLevel(pAgentConfig);
        configureConsoleHandler(pAgentConfig);
        configureFileHandler(pAgentConfig);
    }

    private void configureFileHandler(AgentConfig agentConfig) {
        String logFileName = getLogFileName(agentConfig);
        if (logFileName == null) {
            return;
        }
        rootLogger.addConsoleAppender();
        if (canWriteLogFile(logFileName)) {
            configureFileHandler(logFileName, agentConfig);
        } else {
            rootLogger.warning(MessageFormat.format(
                    "New Relic Agent: Unable to write log file: {0}. Please check permissions on the file and directory.",
                    logFileName));
        }
    }

    private String getLogFileName(AgentConfig agentConfig) {
        File logFile = LogFileHelper.getLogFile(agentConfig);
        return logFile == null ? null : logFile.getPath();
    }

    private void configureLogLevel(AgentConfig agentConfig) {
        if (agentConfig.isDebugEnabled()) {
            rootLogger.setLevel(Level.TRACE.toString().toLowerCase());
        } else {
            rootLogger.setLevel(agentConfig.getLogLevel());
        }
    }

    private void configureConsoleHandler(AgentConfig agentConfig) {
        if (agentConfig.isDebugEnabled() || agentConfig.isLoggingToStdOut()) {
            addConsoleHandler();
        } else {
            rootLogger.removeConsoleAppender();
        }
    }

    private void configureFileHandler(String logFileName, AgentConfig agentConfig) {
        // write logging success or failure to stdout
        rootLogger.info(MessageFormat.format("New Relic Agent: Writing to log file: {0}", logFileName));
        rootLogger.removeConsoleAppender();
        int limit = agentConfig.getLogLimit() * 1024;
        int fileCount = Math.max(1, agentConfig.getLogFileCount());
        String path = LogFileHelper.getLogFile(agentConfig).getParent();
        boolean isDaily = agentConfig.isLogDaily();

        rootLogger.addFileAppender(logFileName, limit, fileCount, isDaily, path);
        logFilePath = logFileName;
        String msg = MessageFormat.format("Writing to New Relic log file: {0}", logFileName);
        rootLogger.info(msg);

        rootLogger.info(MessageFormat.format("JRE vendor {0} version {1}", System.getProperty("java.vendor"),
                System.getProperty("java.version")));
        rootLogger.info(MessageFormat.format("JVM vendor {0} {1} version {2}",
                System.getProperty("java.vm.vendor"), System.getProperty("java.vm.name"),
                System.getProperty("java.vm.version")));
        rootLogger.fine(
                MessageFormat.format("JVM runtime version {0}", System.getProperty("java.runtime.version")));
        rootLogger.info(MessageFormat.format("OS {0} version {1} arch {2}", System.getProperty("os.name"),
                System.getProperty("os.version"), System.getProperty("os.arch")));
    }

    private boolean canWriteLogFile(String logFileName) {
        try {
            File logFile = new File(logFileName);
            if (!logFile.exists()) {
                if (null != logFile.getParentFile()) {
                    logFile.getParentFile().mkdirs();
                }
                logFile.createNewFile();
            }
            return logFile.canWrite();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void addConsoleHandler() {
        rootLogger.addConsoleAppender();
    }

    @Override
    public void setLogLevel(String pLevel) {
        rootLogger.setLevel(pLevel);
    }

    @Override
    public String getLogLevel() {
        return rootLogger.getLevel();
    }

    public static Log4jLogManager create(String name) {
        return new Log4jLogManager(name);
    }

}
