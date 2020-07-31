/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.deadlock;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.deadlock.DeadlockDetectorService;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DeadlockServiceTest {

    @Test
    public void testServiceEnabledByDefault() {
        MockServiceManager mockServiceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(mockServiceManager);
        mockServiceManager.setConfigService(Mockito.mock(ConfigService.class));

        Map<String, Object> root = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);

        DeadlockDetectorService dds = new DeadlockDetectorService();
        Assert.assertTrue(dds.isEnabled());
    }

    @Test
    public void testServiceEnabledExplicitly() {
        MockServiceManager mockServiceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(mockServiceManager);
        mockServiceManager.setConfigService(Mockito.mock(ConfigService.class));

        Map<String, Object> root = new HashMap<>();
        Map<String, Object> deadlockDetector = new HashMap<>();
        deadlockDetector.put("enabled", true);
        root.put("deadlock_detector", deadlockDetector);

        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);

        DeadlockDetectorService dds = new DeadlockDetectorService();
        Assert.assertTrue(dds.isEnabled());
    }

    @Test
    public void testServiceDisabled() throws Exception {
        MockServiceManager mockServiceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(mockServiceManager);
        mockServiceManager.setConfigService(Mockito.mock(ConfigService.class));

        Map<String, Object> root = new HashMap<>();
        Map<String, Object> deadlockDetector = new HashMap<>();
        deadlockDetector.put("enabled", false);
        root.put("deadlock_detector", deadlockDetector);

        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);

        DeadlockDetectorService dds = new DeadlockDetectorService();
        Assert.assertFalse(dds.isEnabled());

        // Check that when the service is disabled, we don't allocate internal state

        Class<?> ddsClass = dds.getClass();

        Field scheduledExecutorField = ddsClass.getDeclaredField("scheduledExecutor");
        scheduledExecutorField.setAccessible(true);
        Assert.assertNull(scheduledExecutorField.get(dds));

        Field deadlockTaskField = ddsClass.getDeclaredField("deadlockTask");
        deadlockTaskField.setAccessible(true);
        Assert.assertNull(deadlockTaskField.get(dds));
    }
}
