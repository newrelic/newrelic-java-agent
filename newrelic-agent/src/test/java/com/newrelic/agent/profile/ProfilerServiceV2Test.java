/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.LatchingRunnable;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ProfilerServiceV2Test {

    private MockServiceManager createServiceManager(Map<String, Object> map) throws Exception {

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
        serviceManager.setConfigService(configService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        ProfilerService profilerService = new ProfilerService();
        serviceManager.setProfilerService(profilerService);

        return serviceManager;
    }

    @Test
    public void enabledByDefault() throws Exception {
        MockServiceManager serviceManager = createServiceManager(Collections.EMPTY_MAP);
        Assert.assertTrue(serviceManager.getProfilerService().isEnabled());
    }

    @Test
    public void enabled() throws Exception {
        MockServiceManager serviceManager = createServiceManager(createAgentConfig(true));
        Assert.assertTrue(serviceManager.getProfilerService().isEnabled());
    }

    @Test
    public void disabled() throws Exception {
        Map<String, Object> config = createAgentConfig(false);
        MockServiceManager serviceManager = createServiceManager(config);
        Assert.assertTrue(!serviceManager.getProfilerService().isEnabled());
    }

    @Test
    public void startProfiler() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        MockServiceManager serviceManager = createServiceManager(createAgentConfig(true));

        MockRPMService rpmService = new MockRPMService();
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        ProfilerService profilerService = new ProfilerService() {
            @Override
            void sessionCompleted(ProfileSession session) {
                super.sessionCompleted(session);
                latch.countDown();
            }
        };
        serviceManager.setProfilerService(profilerService);

        Assert.assertNull(profilerService.getCurrentSession());
        ProfilerParameters parameters = new ProfilerParameters(0L, 50L, 500L, false, false, Agent.isDebugEnabled(),
                null, null);
        parameters.setProfilerFormat("v2");
        profilerService.startProfiler(parameters);
        latch.await(10000, TimeUnit.MILLISECONDS);
        Assert.assertNull(profilerService.getCurrentSession());
        Assert.assertEquals(1, rpmService.getProfilesV2().size());
    }

    @Test
    public void stopProfiler() throws Exception {
        MockServiceManager serviceManager = createServiceManager(createAgentConfig(true));

        MockRPMService rpmService = new MockRPMService();
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        long profileId = 5;
        ProfilerParameters parameters = new ProfilerParameters(profileId, 100L, 200000L, false, false, false, null,
                null);
        parameters.setProfilerFormat("v2");
        ProfilerService profilerService = serviceManager.getProfilerService();
        profilerService.startProfiler(parameters);
        Thread.sleep(1000);
        profilerService.stopProfiler(profileId, false);

        LatchingRunnable.drain(profilerService.getScheduledExecutorService());
        Assert.assertNull(profilerService.getCurrentSession());
        Assert.assertEquals(0, rpmService.getProfiles().size());

        profilerService.doStop();
    }

    @Test
    public void stopProfilerAndReportData() throws Exception {
        MockServiceManager serviceManager = createServiceManager(createAgentConfig(true));

        MockRPMService rpmService = new MockRPMService();
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        long profileId = 5;
        ProfilerParameters parameters = new ProfilerParameters(profileId, 100L, 200000L, false, false, false, null,
                null);
        parameters.setProfilerFormat("v2");
        ProfilerService profilerService = serviceManager.getProfilerService();
        profilerService.startProfiler(parameters);
        Thread.sleep(1000);
        profilerService.stopProfiler(profileId, true);

        LatchingRunnable.drain(profilerService.getScheduledExecutorService());
        Assert.assertNull(profilerService.getCurrentSession());
        Assert.assertEquals(1, rpmService.getProfilesV2().size());
    }

    @Test
    public void testOneSampleProfile() throws Exception {
        MockServiceManager serviceManager = createServiceManager(createAgentConfig(true));

        MockRPMService rpmService = new MockRPMService();
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        ProfilerParameters parameters = new ProfilerParameters(5L, 500L, 500L, false, false, false, null, null);
        parameters.setProfilerFormat("v2");
        ProfilerService profilerService = serviceManager.getProfilerService();
        profilerService.startProfiler(parameters);

        LatchingRunnable.drain(profilerService.getScheduledExecutorService());
        Assert.assertNull(profilerService.getCurrentSession());
        Assert.assertEquals(1, rpmService.getProfilesV2().size());
    }

    @Test
    public void testAgentInstrumentationSample() throws Exception {
        MockServiceManager serviceManager = createServiceManager(createAgentConfig(true));

        MockRPMService rpmService = new MockRPMService();
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        ProfilerParameters parameters = new ProfilerParameters(5L, 500L, 500L, false, false, true, null, null);
        parameters.setProfilerFormat("v2");
        ProfilerService profilerService = serviceManager.getProfilerService();
        profilerService.startProfiler(parameters);

        LatchingRunnable.drain(profilerService.getScheduledExecutorService());
        Assert.assertNull(profilerService.getCurrentSession());
        Assert.assertEquals(1, rpmService.getProfilesV2().size());
    }

    static Map<String, Object> createAgentConfig(final boolean profilerEnabled) {
        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> profileConfig = new HashMap<>();
        profileConfig.put("enabled", profilerEnabled);
        configMap.put("thread_profiler", profileConfig);
        configMap.put("app_name", "Test");
        return configMap;
    }
}
