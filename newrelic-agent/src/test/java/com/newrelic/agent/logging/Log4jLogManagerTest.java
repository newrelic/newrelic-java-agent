package com.newrelic.agent.logging;

import com.newrelic.agent.config.AgentConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class Log4jLogManagerTest {
    Log4jLogManager logManager;
    AgentConfig mockAgentConfig;
    @Before
    public void setup(){
        logManager = Log4jLogManager.create("quizzy bees");
        mockAgentConfig = Mockito.mock(AgentConfig.class);
        Mockito.when(mockAgentConfig.getLogFileName()).thenReturn("test"); //default mock value 'null' causes NPE
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
    public void configureLogLevel_agentConfigInvalid_shouldDefaultToInfo(){
        Mockito.when(mockAgentConfig.isDebugEnabled()).thenReturn(false);

        Mockito.when(mockAgentConfig.getLogLevel()).thenReturn("hogwash");
        logManager.configureLogger(mockAgentConfig);
        assertEquals("info", logManager.getLogLevel().toLowerCase());

        Mockito.when(mockAgentConfig.getLogLevel()).thenReturn(null);
        logManager.configureLogger(mockAgentConfig);
        assertEquals("info", logManager.getLogLevel().toLowerCase());
    }

    @Test
    public void configureLogger_shouldHandle_loggingToStdOut(){
        Mockito.when(mockAgentConfig.isLoggingToStdOut()).thenReturn(true);
        Mockito.when(mockAgentConfig.isDebugEnabled()).thenReturn(false);

        logManager.configureLogger(mockAgentConfig); //shouldn't throw

    }


}