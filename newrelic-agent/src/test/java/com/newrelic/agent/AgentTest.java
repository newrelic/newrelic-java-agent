/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.AgentControlIntegrationConfig;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.instrument.Instrumentation;
public class AgentTest {

    static IAgentLogger logger;
    static MockedStatic<AgentLogManager> agentLogManager;;

    Instrumentation instrumentation = Mockito.mock(Instrumentation.class);

    @BeforeClass
    public static void setup() {
        logger = Mockito.mock(IAgentLogger.class);
        agentLogManager = Mockito.mockStatic(AgentLogManager.class);
        agentLogManager.when(AgentLogManager::getLogger).thenReturn(logger);

    }
    @AfterClass
    public static void teardown() {
        agentLogManager.close();
    }

    @Test
    public void test_agentDisabled1() throws NoSuchFieldException, IllegalAccessException {
        assertLogMessageFromProperty("newrelic.config.agent_enabled", "false",
                "New Relic Agent is disabled by a system property");
    }

    @Test
    public void test_JRockitMessage() throws NoSuchFieldException, IllegalAccessException {
        assertLogMessageFromProperty("java.vm.name", "Oracle JRockit",
                "does not support the Oracle JRockit JVM (\"Oracle JRockit\").");
    }

    @Test
    public void test_emptyLicense() {
        try (MockedStatic<ConfigServiceFactory> csfMock = Mockito.mockStatic(ConfigServiceFactory.class)) {
            AgentConfig agentConfig = Mockito.mock(AgentConfig.class);
            Mockito.when(agentConfig.getAgentControlIntegrationConfig()).thenReturn(Mockito.mock(AgentControlIntegrationConfig.class));
            Mockito.when(agentConfig.getLicenseKey()).thenReturn(null);
            ConfigService configService = Mockito.mock(ConfigService.class);
            Mockito.when(configService.getDefaultAgentConfig()).thenReturn(agentConfig);
            csfMock.when(() -> ConfigServiceFactory.createConfigService(Mockito.any(), Mockito.anyBoolean())).thenReturn(configService);

            Agent.continuePremain("", instrumentation, System.currentTimeMillis());
            Mockito.verify(logger).error(Mockito.contains("license_key is empty in the config. Not starting New Relic Agent"));
        }
        ServiceFactory.setServiceManager(null); // reset
    }

    @Test
    public void test_agentDisabled2() {
        try (MockedStatic<ConfigServiceFactory> csfMock = Mockito.mockStatic(ConfigServiceFactory.class)) {
            AgentConfig agentConfig = Mockito.mock(AgentConfig.class);
            Mockito.when(agentConfig.getAgentControlIntegrationConfig()).thenReturn(Mockito.mock(AgentControlIntegrationConfig.class));
            Mockito.when(agentConfig.getLicenseKey()).thenReturn("licenseKey");
            Mockito.when(agentConfig.isAgentEnabled()).thenReturn(false);
            ConfigService configService = Mockito.mock(ConfigService.class);
            Mockito.when(configService.getDefaultAgentConfig()).thenReturn(agentConfig);
            csfMock.when(() -> ConfigServiceFactory.createConfigService(Mockito.any(), Mockito.anyBoolean())).thenReturn(configService);

            Agent.continuePremain("", instrumentation, System.currentTimeMillis());
            Mockito.verify(logger).warning(Mockito.contains("agent_enabled is false in the config. Not starting New Relic Agent"));
        }
        ServiceFactory.setServiceManager(null); // reset
    }

    @Test
    public void test_agentAlreadyRunning() {
        ServiceManager serviceManager = Mockito.mock(ServiceManager.class);
        ServiceFactory.setServiceManager(serviceManager);

        Agent.continuePremain("", instrumentation, System.currentTimeMillis());
        Mockito.verify(logger).warning(Mockito.contains("New Relic Agent is already running"));

        ServiceFactory.setServiceManager(null); // reset
    }

    private void assertLogMessageFromProperty(String propertyName, String value, String logMessage) throws NoSuchFieldException, IllegalAccessException {
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        String oldValue = System.getProperty(propertyName);
        System.setProperty(propertyName, value);
        Agent.continuePremain("", instrumentation, System.currentTimeMillis());
        Mockito.verify(logger).warning(Mockito.contains(logMessage));
        if (oldValue == null) {
            System.clearProperty(propertyName);
        } else {
            System.setProperty(propertyName, oldValue);
        }
    }

}
