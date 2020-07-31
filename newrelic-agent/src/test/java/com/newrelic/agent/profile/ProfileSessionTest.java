/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

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

public class ProfileSessionTest {

    private MockServiceManager createServiceManager(Map<String, Object> config) throws Exception {

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config), config);
        serviceManager.setConfigService(configService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        ProfilerService profilerService = new ProfilerService();
        serviceManager.setProfilerService(profilerService);

        return serviceManager;
    }

    @Test
    public void singleSample() throws Exception {
        Map<String, Object> config = ProfilerServiceTest.createAgentConfig(true);
        MockServiceManager serviceManager = createServiceManager(config);

        MockRPMService rpmService = new MockRPMService();
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        ProfilerService profilerService = serviceManager.getProfilerService();
        ProfilerParameters parameters = new ProfilerParameters(5L, 100L, 100L, false, false, false, null, null);
        ProfileSession session = new ProfileSession(profilerService, parameters);
        session.start();
        LatchingRunnable.drain(profilerService.getScheduledExecutorService());

        Assert.assertTrue(session.isDone());
        Assert.assertNotNull(session.getProfile());
        Assert.assertEquals(1, session.getProfile().getSampleCount());
        Assert.assertEquals(parameters.getProfileId(), session.getProfileId());

        Assert.assertNull(profilerService.getCurrentSession());
        Assert.assertEquals(1, rpmService.getProfiles().size());
    }

    @Test
    public void multiSample() throws Exception {
        Map<String, Object> config = ProfilerServiceTest.createAgentConfig(true);
        MockServiceManager serviceManager = createServiceManager(config);

        MockRPMService rpmService = new MockRPMService();
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        ProfilerService profilerService = serviceManager.getProfilerService();
        ProfilerParameters parameters = new ProfilerParameters(5L, 100L, 500L, false, false, false, null, null);
        ProfileSession session = new ProfileSession(profilerService, parameters);
        session.start();
        Thread.sleep(1000L);

        Assert.assertTrue(session.isDone());
        Assert.assertNotNull(session.getProfile());
        Assert.assertTrue(session.getProfile().getSampleCount() > 1);
        Assert.assertEquals(parameters.getProfileId(), session.getProfileId());

        Assert.assertNull(profilerService.getCurrentSession());
        Assert.assertEquals(1, rpmService.getProfiles().size());
    }

    @Test
    public void stopSession() throws Exception {
        Map<String, Object> config = ProfilerServiceTest.createAgentConfig(true);
        MockServiceManager serviceManager = createServiceManager(config);

        MockRPMService rpmService = new MockRPMService();
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        ProfilerService profilerService = serviceManager.getProfilerService();
        ProfilerParameters parameters = new ProfilerParameters(5L, 100L, 300000L, false, false, false, null, null);
        ProfileSession session = new ProfileSession(profilerService, parameters);
        session.start();

        Thread.sleep(1000);
        session.stop(true);
        LatchingRunnable.drain(profilerService.getScheduledExecutorService());

        Assert.assertTrue(session.isDone());
        Assert.assertNotNull(session.getProfile());
        Assert.assertTrue(session.getProfile().getSampleCount() > 1);
        Assert.assertEquals(parameters.getProfileId(), session.getProfileId());

        Assert.assertNull(profilerService.getCurrentSession());
        Assert.assertEquals(1, rpmService.getProfiles().size());
    }

    @Test
    public void stopSessionNoReport() throws Exception {
        Map<String, Object> config = ProfilerServiceTest.createAgentConfig(true);
        MockServiceManager serviceManager = createServiceManager(config);

        MockRPMService rpmService = new MockRPMService();
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        ProfilerService profilerService = serviceManager.getProfilerService();
        ProfilerParameters parameters = new ProfilerParameters(5L, 100L, 300000L, false, false, false, null, null);
        ProfileSession session = new ProfileSession(profilerService, parameters);
        session.start();

        Thread.sleep(1000);
        session.stop(false);
        LatchingRunnable.drain(profilerService.getScheduledExecutorService());

        Assert.assertTrue(session.isDone());
        Assert.assertNotNull(session.getProfile());
        Assert.assertTrue(session.getProfile().getSampleCount() > 1);
        Assert.assertEquals(parameters.getProfileId(), session.getProfileId());

        Assert.assertNull(profilerService.getCurrentSession());
        Assert.assertTrue(rpmService.getProfiles().isEmpty());
    }

}
