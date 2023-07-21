package com.newrelic.agent.logging;

import com.newrelic.agent.config.AgentConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


import static org.junit.Assert.*;

public class Log4jLogManagerTest {
    Log4jLogManager logManager;
    AgentConfig mockAgentConfig;

    private String CONSOLE_APPENDER_NAME = "Console"; //this is declared private static in Log4jLogger

    private String FILE_APPENDER_NAME = FileAppenderFactory.FILE_APPENDER_NAME;
    @Before
    public void setup(){
        logManager = Log4jLogManager.create("quizzy bees");
        mockAgentConfig = Mockito.mock(AgentConfig.class);
        Mockito.when(mockAgentConfig.getLogFileName()).thenReturn("test_file"); //default mock value 'null' causes NPE
    }

    @Test
    public void logLevel_debugEnabled_shouldBeTrace(){
        Mockito.when(mockAgentConfig.isDebugEnabled()).thenReturn(true);
        logManager.configureLogger(mockAgentConfig);
        assertEquals("trace", logManager.getLogLevel().toLowerCase());

    }

    @Test
    public void logLevel_debugDisabled_shouldMatchAgentConfig(){
        Mockito.when(mockAgentConfig.isDebugEnabled()).thenReturn(false);

        Mockito.when(mockAgentConfig.getLogLevel()).thenReturn("INFO");
        logManager.configureLogger(mockAgentConfig);
        assertEquals("info", logManager.getLogLevel().toLowerCase());

        Mockito.when(mockAgentConfig.getLogLevel()).thenReturn("error");
        logManager.configureLogger(mockAgentConfig);
        assertEquals("error", logManager.getLogLevel().toLowerCase());

        Mockito.when(mockAgentConfig.getLogLevel()).thenReturn("warn");
        logManager.configureLogger(mockAgentConfig);
        assertEquals("warn", logManager.getLogLevel().toLowerCase());

        Mockito.when(mockAgentConfig.getLogLevel()).thenReturn("finest");
        logManager.configureLogger(mockAgentConfig);
        assertEquals("trace", logManager.getLogLevel().toLowerCase());
    }

    @Test
    public void configureLogLevel_agentConfigInvalid_shouldBeSetToDefault(){
        Mockito.when(mockAgentConfig.isDebugEnabled()).thenReturn(false);
        String DEFAULT_LOG_LEVEL = "info";

        Mockito.when(mockAgentConfig.getLogLevel()).thenReturn("hogwash");
        logManager.configureLogger(mockAgentConfig);
        assertEquals(DEFAULT_LOG_LEVEL, logManager.getLogLevel().toLowerCase());

        Mockito.when(mockAgentConfig.getLogLevel()).thenReturn(null);
        logManager.configureLogger(mockAgentConfig);
        assertEquals(DEFAULT_LOG_LEVEL, logManager.getLogLevel().toLowerCase());
    }

    @Test
    public void configureConsoleHandler_shouldAddConsoleAppender_whenLoggingToStdOut(){
        Mockito.when(mockAgentConfig.isLoggingToStdOut()).thenReturn(true);
        Mockito.when(mockAgentConfig.isDebugEnabled()).thenReturn(false);

        logManager.configureLogger(mockAgentConfig);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("quizzy bees");

        assertNotNull(loggerConfig.getAppenders().get(CONSOLE_APPENDER_NAME));

    }

    @Test
    public void configureConsoleHandler_shouldNotAddConsoleAppender_otherwise(){
        Mockito.when(mockAgentConfig.isLoggingToStdOut()).thenReturn(false);
        Mockito.when(mockAgentConfig.isDebugEnabled()).thenReturn(false);

        logManager.configureLogger(mockAgentConfig);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("quizzy bees");

        assertNull(loggerConfig.getAppenders().get(CONSOLE_APPENDER_NAME));

    }

    @Test
    public void configureFileHandler_shouldAddFileAppender(){
        Mockito.when(mockAgentConfig.isLoggingToStdOut()).thenReturn(false);
        Mockito.when(mockAgentConfig.isDebugEnabled()).thenReturn(false);

        logManager.configureLogger(mockAgentConfig);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("quizzy bees");

        assertNotNull(loggerConfig.getAppenders().get(FILE_APPENDER_NAME));

    }


}