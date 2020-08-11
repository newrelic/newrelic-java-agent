package com.newrelic.agent.autoname;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.autoname.ApplicationAutoName;
import com.newrelic.agent.config.EnvironmentFacade;
import com.newrelic.agent.discovery.AgentArguments;
import com.newrelic.bootstrap.BootstrapAgent;

public class ApplicationAutoNameTest {
    @After
    public void after() {
        System.getProperties().remove(BootstrapAgent.NR_AGENT_ARGS_SYSTEM_PROPERTY);
    }

    @Test
    public void getMarathonAppName() {
        EnvironmentFacade environmentFacade = Mockito.mock(EnvironmentFacade.class);
        String imageName = "cf-registry.nr-ops.net/collector-collective/connect-service-account-data-stream-filter:release-39";
        Mockito.when(environmentFacade.getenv("MARATHON_APP_DOCKER_IMAGE")).thenReturn(imageName);

        System.setProperty(BootstrapAgent.NR_AGENT_ARGS_SYSTEM_PROPERTY, "{}");
        ApplicationAutoName name = ApplicationAutoName.getApplicationAutoName(environmentFacade);
        assertEquals("cf-registry.nr-ops.net/collector-collective/connect-service-account-data-stream-filter",
                name.getName(environmentFacade));
    }

    @Test
    public void fallbackToCommandLine() {
        EnvironmentFacade environmentFacade = Mockito.mock(EnvironmentFacade.class);
        System.setProperty(BootstrapAgent.NR_AGENT_ARGS_SYSTEM_PROPERTY, "{}");
        String commandLine = "app.jar";
        Mockito.when(environmentFacade.getenv(AgentArguments.NEW_RELIC_COMMAND_LINE_ENV_VARIABLE))
            .thenReturn(commandLine);

        ApplicationAutoName name = ApplicationAutoName.getApplicationAutoName(environmentFacade);
        assertEquals("app.jar",
                name.getName(environmentFacade));
    }

    @Test
    public void tomcatOneContext() {
        assertEquals("My App",
            ApplicationAutoName.AppServer.Tomcat.getName(
                ImmutableSet.of(new ApplicationAutoName.ServletContextDetails("mine", "My App"))));
    }

    @Test
    public void tomcatOneContextPlusDefaults() {
        assertEquals("My App",
            ApplicationAutoName.AppServer.Tomcat.getName(
                ImmutableSet.of(
                        new ApplicationAutoName.ServletContextDetails("examples", "Tomcat Examples"),
                        new ApplicationAutoName.ServletContextDetails("mine", "My App"))));
    }
}
