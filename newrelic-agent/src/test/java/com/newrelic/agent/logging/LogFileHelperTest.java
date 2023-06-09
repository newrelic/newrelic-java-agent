package com.newrelic.agent.logging;

import com.newrelic.agent.config.AgentConfig;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

public class LogFileHelperTest {

    @Test
    public void getLogFile_loggingToStdout_returnsNull() {
        AgentConfig agentConfig = Mockito.mock(AgentConfig.class);
        Mockito.when(agentConfig.isLoggingToStdOut()).thenReturn(true);

        Assert.assertNull(LogFileHelper.getLogFile(agentConfig));
    }

    @Test
    public void getLogFile_withLogFilePropertySet_returnsFileObj() {
        AgentConfig agentConfig = Mockito.mock(AgentConfig.class);
        System.setProperty("newrelic.logfile", "./test.log");
        Mockito.when(agentConfig.isLoggingToStdOut()).thenReturn(false);

        Assert.assertNotNull(LogFileHelper.getLogFile(agentConfig));

        System.clearProperty("newrelic.logfile");
        new File("./test.log").deleteOnExit();
    }

    @Test
    public void getLogFile_withNoLogFilePropertySetAndAgentLogConfigured_returnsFileObj() {
        AgentConfig agentConfig = Mockito.mock(AgentConfig.class);
        Mockito.when(agentConfig.isLoggingToStdOut()).thenReturn(false);
        Mockito.when(agentConfig.getLogFileName()).thenReturn("test.log");
        Mockito.when(agentConfig.getLogFilePath()).thenReturn("./");

        Assert.assertNotNull(LogFileHelper.getLogFile(agentConfig));

        new File("./test.log").deleteOnExit();
    }

    @Test
    public void getLogFile_withNoLogFilePropertySetAndAgentLogConfiguredWithoutFolder_returnsFileObj() {
        AgentConfig agentConfig = Mockito.mock(AgentConfig.class);
        Mockito.when(agentConfig.isLoggingToStdOut()).thenReturn(false);
        Mockito.when(agentConfig.getLogFileName()).thenReturn("test.log");

        File f = LogFileHelper.getLogFile(agentConfig);
        Assert.assertNotNull(f);

        f.deleteOnExit();
    }
}
