/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DataSenderImplConnectCycleTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Mock
    public StatsService mockStatsService;

    @After
    public void tearDown() {
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void testFullPreconnectConnectCycleNoSecurity() throws Exception {
        MockitoAnnotations.initMocks(this);

        final Map<String, Object> settings = new HashMap<>();
        settings.put(AgentConfigImpl.APP_NAME, "Unit Test");
        settings.put(AgentConfigImpl.HOST, "no-collector.example.com");

        final MockServiceManager serviceManager = new MockServiceManager(
                ConfigServiceFactory.createConfigServiceUsingSettings(settings)
        );
        serviceManager.setStatsService(mockStatsService);

        HttpClientWrapper wrapper = new ConnectCycleNoSecuritySuccessClientWrapper();

        DataSenderImpl target = new DataSenderImpl(serviceManager.getConfigService().getDefaultAgentConfig(), wrapper, null, Agent.LOG,
                ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        Map<String, Object> startupOptions = new HashMap<>();
        startupOptions.put("test-sentinel", "test-value");
        Map<String, Object> result = target.connect(startupOptions);
        assertEquals("my-run-id", target.getAgentRunId());
        assertEquals("value", result.get("other"));
    }

    @Test
    public void testFullPreconnectConnectCycleWithLasp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final Map<String, Object> settings = new HashMap<>();
        settings.put(AgentConfigImpl.APP_NAME, "Unit Test");
        settings.put(AgentConfigImpl.HOST, "no-collector.example.com");
        settings.put(AgentConfigImpl.LASP_TOKEN, "ffff-ffff-ffff-ffff");

        final MockServiceManager serviceManager = new MockServiceManager(
                ConfigServiceFactory.createConfigServiceUsingSettings(settings)
        );
        serviceManager.setStatsService(mockStatsService);

        HttpClientWrapper wrapper = new ConnectCycleLaspSuccessClientWrapper();

        DataSenderImpl target = new DataSenderImpl(serviceManager.getConfigService().getDefaultAgentConfig(), wrapper, null, Agent.LOG,
                ServiceFactory.getConfigService());
        target.setAgentRunId("agent run id");

        Map<String, Object> startupOptions = new HashMap<>();
        startupOptions.put("test-sentinel", "test-value");
        Map<String, Object> result = target.connect(startupOptions);
        assertEquals("my-run-id", target.getAgentRunId());
        assertEquals("value", result.get("other"));
    }

}
