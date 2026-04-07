/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.service.module.JarCollectorService;
import com.newrelic.api.agent.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class ServiceManagerTest {

    @Test
    public void dynamicServices() throws Exception {
        AgentHelper.initializeConfig();
        CoreService agent = new MockCoreService();
        ConfigService configService = ConfigServiceFactory.createConfigService(mock(Logger.class), false);
        ServiceManager serviceManager = new ServiceManagerImpl(agent, configService);
        Service testService = new TestService("My Service");
        serviceManager.addService(testService);
        Assert.assertEquals(testService, serviceManager.getService(testService.getName()));
        Assert.assertFalse(testService.isStarted());
    }

    @Test
    public void jarCollectorDisabledWhenServerlessModeEnabled() throws Exception {
        AgentHelper.initializeConfig();

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(AgentConfigImpl.APP_NAME, "Test App");
        configMap.put("serverless_mode", Collections.singletonMap("enabled", true));
        configMap.put("jar_collector", Collections.singletonMap("enabled", true));

        MockCoreService mockCoreService = new MockCoreService();
        mockCoreService.setInstrumentation(Mockito.mock(InstrumentationProxy.class));
        Mockito.when(mockCoreService.getInstrumentation().getAllLoadedClasses()).thenReturn(new Class[] {});

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);

        ServiceManager serviceManager = new ServiceManagerImpl(mockCoreService, configService);
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        JarCollectorService jarCollectorService = serviceManager.getJarCollectorService();

        Assert.assertFalse("Jar collector should be disabled when serverless mode is enabled",
                jarCollectorService.isEnabled());

        serviceManager.stop();
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void jarCollectorEnabledWhenServerlessModeDisabled() throws Exception {
        AgentHelper.initializeConfig();

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(AgentConfigImpl.APP_NAME, "Test App");
        configMap.put("serverless_mode", Collections.singletonMap("enabled", false));
        configMap.put("jar_collector", Collections.singletonMap("enabled", true));

        MockCoreService mockCoreService = new MockCoreService();
        mockCoreService.setInstrumentation(Mockito.mock(InstrumentationProxy.class));
        Mockito.when(mockCoreService.getInstrumentation().getAllLoadedClasses()).thenReturn(new Class[] {});

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);

        ServiceManager serviceManager = new ServiceManagerImpl(mockCoreService, configService);
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        JarCollectorService jarCollectorService = serviceManager.getJarCollectorService();

        Assert.assertTrue("Jar collector should be enabled when serverless mode is disabled and jar_collector config is enabled",
                jarCollectorService.isEnabled());

        serviceManager.stop();
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void jarCollectorDisabledWhenExplicitlyDisabledRegardlessOfServerlessMode() throws Exception {
        AgentHelper.initializeConfig();

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(AgentConfigImpl.APP_NAME, "Test App");
        configMap.put("serverless_mode", Collections.singletonMap("enabled", false));
        configMap.put("jar_collector", Collections.singletonMap("enabled", false));

        MockCoreService mockCoreService = new MockCoreService();
        mockCoreService.setInstrumentation(Mockito.mock(InstrumentationProxy.class));
        Mockito.when(mockCoreService.getInstrumentation().getAllLoadedClasses()).thenReturn(new Class[] {});

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);

        ServiceManager serviceManager = new ServiceManagerImpl(mockCoreService, configService);
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        JarCollectorService jarCollectorService = serviceManager.getJarCollectorService();

        Assert.assertFalse("Jar collector should be disabled when explicitly disabled in config",
                jarCollectorService.isEnabled());

        serviceManager.stop();
        ServiceFactory.setServiceManager(null);
    }

    private static class TestService extends AbstractService {

        private TestService(String name) {
            super(name);
        }

        @Override
        protected void doStart() {
        }

        @Override
        protected void doStop() {
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

    }

}
