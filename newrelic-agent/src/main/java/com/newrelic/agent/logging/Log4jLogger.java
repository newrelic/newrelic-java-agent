/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.service.ServiceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_CLASS;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_STACK;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LEVEL;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LOGGER_NAME;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_ID;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_NAME;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.TIMESTAMP;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.UNKNOWN;
import static com.newrelic.agent.logging.FileAppenderFactory.FILE_APPENDER_NAME;

class Log4jLogger implements IAgentLogger {

    /**
     * The name of the console appender.
     */
    private static final String CONSOLE_APPENDER_NAME = "Console";

    /**
     * The pattern to use for log messages.
     */
    static final String CONVERSION_PATTERN = "%d{ISO8601_OFFSET_DATE_TIME_HHMM} [%pid %tid] %logger %marker: %m%n";

    /**
     * The logger for this Log4jLogger.
     */
    private final Logger logger;

    /**
     * Agent logger name
     */
    private static final String AGENT_LOGGER_NAME = "agent_logger";

    private final Map<String, IAgentLogger> childLoggers = new ConcurrentHashMap<>();

    /**
     * Creates this Log4jLogger.
     *
     * @param name Name of this logger.
     */
    private Log4jLogger(final String name, boolean isAgentRoot) {
        logger = LogManager.getLogger(name);

        if (isAgentRoot) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
            loggerConfig.setAdditive(false);
            ctx.updateLoggers();
            FineFilter.getFineFilter().start();
        }
    }

    @Override
    public void severe(String pMessage) {
        severe(true, pMessage);
    }

    @Override
    public void error(String pMessage) {
        error(true, pMessage);
    }

    @Override
    public void warning(String pMessage) {
        warning(true, pMessage);

    }

    @Override
    public void info(String pMessage) {
        info(true, pMessage);

    }

    @Override
    public void config(String pMessage) {
        config(true, pMessage);
    }

    @Override
    public void fine(String pMessage) {
        fine(true, pMessage);
    }

    @Override
    public void finer(String pMessage) {
        finer(true, pMessage);
    }

    @Override
    public void finest(String pMessage) {
        finest(true, pMessage);
    }

    @Override
    public void debug(String pMessage) {
        debug(true, pMessage);
    }

    @Override
    public void trace(String pMessage) {
        trace(true, pMessage);
    }

    @Override
    public void severe(boolean allowForwarding, String pMessage) {
        logger.error(Log4jMarkers.ERROR_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.ERROR, pMessage);
    }

    @Override
    public void error(boolean allowForwarding, String pMessage) {
        logger.error(Log4jMarkers.ERROR_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.ERROR, pMessage);
    }

    @Override
    public void warning(boolean allowForwarding, String pMessage) {
        logger.warn(Log4jMarkers.WARN_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.WARN, pMessage);
    }

    @Override
    public void info(boolean allowForwarding, String pMessage) {
        logger.info(Log4jMarkers.INFO_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.INFO, pMessage);
    }

    @Override
    public void config(boolean allowForwarding, String pMessage) {
        logger.info(Log4jMarkers.INFO_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.INFO, pMessage);
    }

    @Override
    public void fine(boolean allowForwarding, String pMessage) {
        logger.debug(Log4jMarkers.FINE_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.FINE, pMessage);
    }

    @Override
    public void finer(boolean allowForwarding, String pMessage) {
        logger.debug(Log4jMarkers.FINER_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.FINER, pMessage);
    }

    @Override
    public void finest(boolean allowForwarding, String pMessage) {
        logger.debug(Log4jMarkers.FINEST_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.FINEST, pMessage);
    }

    @Override
    public void debug(boolean allowForwarding, String pMessage) {
        logger.debug(Log4jMarkers.DEBUG_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.DEBUG, pMessage);
    }

    @Override
    public void trace(boolean allowForwarding, String pMessage) {
        logger.debug(Log4jMarkers.TRACE_MARKER, pMessage);
        forward(allowForwarding, Log4jLevel.TRACE, pMessage);
    }

    @Override
    public boolean isFineEnabled() {
        return logger.isDebugEnabled() && FineFilter.getFineFilter().isEnabledFor(Level.FINE);
    }

    @Override
    public boolean isFinerEnabled() {
        return logger.isDebugEnabled() && FineFilter.getFineFilter().isEnabledFor(Level.FINER);
    }

    @Override
    public boolean isFinestEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isLoggable(Level pLevel) {
        Log4jLevel level = Log4jLevel.getLevel(pLevel);
        return level != null && logger.isEnabled(level.getLog4jLevel()) && FineFilter.getFineFilter().isEnabledFor(pLevel);
    }

    @Override
    public void log(Level pLevel, final String pMessage, final Throwable pThrowable) {
        log(pLevel, true, pMessage, pThrowable);
    }

    @Override
    public void log(Level pLevel, String pMessage) {
        log(pLevel, true, pMessage);
    }

    @Override
    public void log(Level pLevel, String pMessage, Object[] pArgs, Throwable pThrowable) {
        log(pLevel, true, pMessage, pArgs, pThrowable);
    }

    @Override
    public void log(Level pLevel, boolean allowForwarding, final String pMessage, final Throwable pThrowable) {
        if (isLoggable(pLevel)) {
            final Log4jLevel level = Log4jLevel.getLevel(pLevel);
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    logger.log(level.getLog4jLevel(), level.getMarker(), pMessage, pThrowable);
                    forward(allowForwarding, level, pMessage, pThrowable);
                    return null;
                }
            });
        }
    }

    @Override
    public void log(Level pLevel, boolean allowForwarding, String pMessage) {
        Log4jLevel level = Log4jLevel.getLevel(pLevel);
        logger.log(level.getLog4jLevel(), level.getMarker(), pMessage);
        forward(allowForwarding, level, pMessage);
    }

    @Override
    public void log(Level pLevel, boolean allowForwarding, String pMessage, Object[] pArgs, Throwable pThrowable) {
        Log4jLevel level = Log4jLevel.getLevel(pLevel);
        Message message = new ParameterizedMessage(pMessage, pArgs);
        logger.log(level.getLog4jLevel(), level.getMarker(), message, pThrowable);
        forward(allowForwarding, level, pMessage, pThrowable);
    }

    @Override
    public IAgentLogger getChildLogger(Class<?> pClazz) {
        return getChildLogger(pClazz.getName());
    }

    @Override
    public IAgentLogger getChildLogger(String pFullName) {
        IAgentLogger logger = Log4jLogger.create(pFullName, false);
        childLoggers.put(pFullName, logger);
        return logger;
    }

    /**
     * Sets the level.
     *
     * @param level The level to set on the logger.
     */
    public void setLevel(String level) {
        Log4jLevel newLevel = Log4jLevel.getLevel(level, Log4jLevel.INFO);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());
        loggerConfig.setLevel(newLevel.getLog4jLevel());
        ctx.updateLoggers();
        FineFilter.getFineFilter().setLevel(newLevel.getJavaLevel());
    }

    /**
     * Gets the level. Finest will be returned as trace.
     *
     * @return The current level of the logger.
     */
    public String getLevel() {
        if (logger.getLevel() == org.apache.logging.log4j.Level.DEBUG) {
            return FineFilter.getFineFilter().getLevel().toString();
        }
        return logger.getLevel().toString();
    }

    /**
     * Removes the console appender.
     */
    public void removeConsoleAppender() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());

        Appender consoleAppender = loggerConfig.getAppenders().get(CONSOLE_APPENDER_NAME);
        if (consoleAppender != null) {
            loggerConfig.removeAppender(CONSOLE_APPENDER_NAME);
        }
    }

    /**
     * Creates a console appender if none exists.
     */
    public void addConsoleAppender() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());

        if (loggerConfig.getAppenders().get(CONSOLE_APPENDER_NAME) != null) {
            return;
        }

        ConsoleAppender consoleAppender = ((ConsoleAppender.Builder) ConsoleAppender.newBuilder()
                .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                .setLayout(PatternLayout.newBuilder().withPattern(CONVERSION_PATTERN).build())
                .setName(CONSOLE_APPENDER_NAME)
                .setFilter(FineFilter.getFineFilter()))
                .build();
        consoleAppender.start();

        loggerConfig.addAppender(consoleAppender, null, FineFilter.getFineFilter());
        ctx.updateLoggers();
    }

    /**
     * Adds a file appender.
     *
     * @param fileName Name of the appender.
     * @param logLimitBytes Log limit
     * @param fileCount The number of files.
     */
    public void addFileAppender(String fileName, long logLimitBytes, int fileCount, boolean isDaily) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());

        if (loggerConfig.getAppenders().get(FILE_APPENDER_NAME) != null) {
            return;
        }

        FileAppenderFactory fileAppenderFactory = new FileAppenderFactory(fileCount, logLimitBytes, fileName, isDaily);
        AbstractOutputStreamAppender<? extends FileManager> fileAppender = fileAppenderFactory.build();
        if (fileAppender == null) {
            return;
        }

        fileAppender.start();
        loggerConfig.addAppender(fileAppender, null, FineFilter.getFineFilter());
        ctx.updateLoggers();
    }

    /**
     * Creates a log4j logger.
     *
     * @param name The name of the logger.
     * @param isAgentRoot True means this is the root logger for the agent.
     * @return The logger created.
     */
    public static Log4jLogger create(String name, boolean isAgentRoot) {
        return new Log4jLogger(name, isAgentRoot);
    }

    @Override
    public void log(Level level, String pattern, Object[] msg) {
        log(level, true, pattern, msg);
    }

    @Override
    public void log(Level level, String pattern, Object part1) {
        log(level, true, pattern, part1);
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2) {
        log(level, true, pattern, part1, part2);
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3) {
        log(level, true, pattern, part1, part2, part3);
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4) {
        log(level, true, pattern, part1, part2, part3, part4);
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) {
        log(level, true, pattern, part1, part2, part3, part4, part5);
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6) {
        log(level, true, pattern, part1, part2, part3, part4, part5, part6);
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6, Object part7) {
        log(level, true, pattern, part1, part2, part3, part4, part5, part6, part7);
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6, Object part7, Object... otherParts) {
        log(level, true, pattern, part1, part2, part3, part4, part5, part6, part7, otherParts);
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object[] msg) {
        log(level, true, t, pattern, msg);
    }

    @Override
    public void log(Level level, Throwable t, String pattern) {
        log(level, true, t, pattern);
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1) {
        log(level, true, t, pattern, part1);
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2) {
        log(level, true, t, pattern, part1, part2);
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3) {
        log(level, true, t, pattern, part1, part2, part3);
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4) {
        log(level, true, t, pattern, part1, part2, part3, part4);
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5) {
        log(level, true, t, pattern, part1, part2, part3, part4, part5);
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5, Object part6) {
        log(level, true, t, pattern, part1, part2, part3, part4, part5, part6);
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5, Object part6, Object part7) {
        log(level, true, t, pattern, part1, part2, part3, part4, part5, part6, part7);
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4,
            Object part5, Object part6, Object part7, Object... otherParts) {
        log(level, true, t, pattern, part1, part2, part3, part4, part5, part6, part7, otherParts);
    }


    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object[] msg) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, msg));
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1));
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2));
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3));
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3, part4));
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3, part4, part5));
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3, part4, part5, part6));
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6,
            Object part7) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3, part4, part5, part6, part7));
        }

    }

    @Override
    public void log(Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6,
            Object part7, Object... otherParts) {
        if (isLoggable(level)) {
            Object[] parts = merge(otherParts, part1, part2, part3, part4, part5, part6, part7);
            log(level, allowForwarding, getMessage(pattern, parts));
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object[] msg) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, msg), t);
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern), t);
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1), t);
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2), t);
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3), t);
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3, part4), t);
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3, part4, part5), t);
        }
    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3, part4, part5, part6), t);
        }

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6, Object part7) {
        if (isLoggable(level)) {
            log(level, allowForwarding, getMessage(pattern, part1, part2, part3, part4, part5, part6, part7), t);
        }

    }

    @Override
    public void log(Level level, boolean allowForwarding, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5,
            Object part6, Object part7, Object... otherParts) {
        if (isLoggable(level)) {
            Object[] parts = merge(otherParts, part1, part2, part3, part4, part5, part6, part7);
            log(level, allowForwarding, getMessage(pattern, parts), t);
        }
    }

    @Override
    public void logToChild(String childName, Level level, boolean allowForwarding, String pattern, Object part1, Object part2, Object part3, Object part4) {
        if (isLoggable(level)) {
            IAgentLogger logger = childLoggers.get(childName);
            if (logger == null) {
                logger = Agent.LOG;
            }
            logger.log(level, allowForwarding, pattern, part1, part2, part3, part4);
        }
    }

    private String getMessage(String pattern, Object... parts) {
        return (parts == null || parts.length == 0 || pattern == null)
                ? pattern
                : MessageFormat.format(pattern, formatValues(parts));
    }

    private Object[] formatValues(Object[] parts) {
        Object[] strings = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) {
            strings[i] = formatValue(parts[i]);
        }
        return strings;
    }

    private Object formatValue(Object obj) {
        if (obj instanceof Class) {
            return ((Class<?>) obj).getName();
        } else if (obj instanceof Throwable) {
            return obj.toString();
        } else {
            return obj;
        }
    }

    private Object[] merge(Object[] otherParts, Object... firstParameters) {
        int otherPartsLength = otherParts != null ? otherParts.length : 0;
        Object[] mergedArray = new Object[firstParameters.length + otherPartsLength];

        System.arraycopy(firstParameters, 0, mergedArray, 0, firstParameters.length);
        if (otherPartsLength > 0) {
            System.arraycopy(otherParts, 0, mergedArray, firstParameters.length, otherPartsLength);
        }

        return mergedArray;
    }

    private void forward(boolean allowForwarding, Log4jLevel level, String message) {
        forward(allowForwarding, level, message, null);
    }

    private void forward(boolean allowForwarding, Log4jLevel level, String message, Throwable throwable) {
        if (level == null) {
            level = Log4jLevel.ALL;
        }
        forward(allowForwarding, level.getJavaLevel().getName(), message, throwable);
    }

    private void forward(boolean allowForwarding, String level, String message, Throwable throwable) {
        if (!allowForwarding) {
            return;
        }
        long timestamp = java.time.Instant.now().toEpochMilli();
        boolean forwardAgentLogsEnabled = false;
        try {
            forwardAgentLogsEnabled = ServiceFactory.getConfigService().getDefaultAgentConfig().getForwardAgentLogs();
        } catch (Exception ignored) {
            // ServiceFactory.getConfigService() can cause an NPE when called before the ServiceManager has been initialized
        }

        if (forwardAgentLogsEnabled && AgentBridge.getAgent().getLogSender() != null) {
            Map<LogAttributeKey, Object> logEventMap = new HashMap<>(7);

            if (level == null || level.isEmpty()) {
                logEventMap.put(LEVEL, UNKNOWN);
            } else {
                logEventMap.put(LEVEL, level);
            }

            logEventMap.put(MESSAGE, message);
            logEventMap.put(TIMESTAMP, timestamp);

            logEventMap.put(LOGGER_NAME, AGENT_LOGGER_NAME);

            String errorStack = ExceptionUtil.getErrorStack(throwable);
            if (errorStack != null) {
                logEventMap.put(ERROR_STACK, errorStack);
            }

            String errorMessage = ExceptionUtil.getErrorMessage(throwable);
            if (errorMessage != null) {
                logEventMap.put(ERROR_MESSAGE, errorMessage);
            }

            String errorClass = ExceptionUtil.getErrorClass(throwable);
            if (errorClass != null) {
                logEventMap.put(ERROR_CLASS, errorClass);
            }

            Thread thread = Thread.currentThread();
            String threadName = thread.getName();
            if (threadName != null) {
                logEventMap.put(THREAD_NAME, threadName);
            }
            logEventMap.put(THREAD_ID, thread.getId());

            AgentBridge.getAgent().getLogSender().recordAgentLogEvent(logEventMap);
        }
    }

    @Override
    public void logToChild(String childName, Level level, String pattern, Object part1, Object part2, Object part3,
            Object part4) {
        logToChild(childName, level, true, pattern, part1, part2, part3, part4);
    }

}
