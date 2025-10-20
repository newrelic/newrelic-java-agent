/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileSamplerTest {

    @BeforeClass
    public static void beforeClass() throws Exception {

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        Map<String, Object> map = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(map);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

    }

    @Test
    public void testSampleStackTraces() {
        ProfilerParameters parameters = new ProfilerParameters(0L, 0L, 0L, false, false, true, null, null);
        IProfile profile = new Profile(parameters);
        List<IProfile> profiles = new ArrayList<>();
        profiles.add(profile);
        ProfileSampler sampler = new ProfileSampler();

        sampler.sampleStackTraces(profiles);

        sampler.sampleStackTraces(profiles);

        Assert.assertEquals(2, profile.getSampleCount());
    }

    @Test
    public void generateTestData() throws Exception {
        ProfilerParameters parameters = new ProfilerParameters(0L, 0L, 0L, false, false, true, null, null);
        IProfile profile = new Profile(parameters);
        List<IProfile> profiles = new ArrayList<>();
        profiles.add(profile);
        ProfileSampler sampler = new ProfileSampler();

        sampler.sampleStackTraces(profiles);

        sampler.sampleStackTraces(profiles);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);
        profile.getProfileTree(ThreadType.BasicThreadType.REQUEST).writeJSONString(writer);
        writer.close();

        System.out.println(out.toString());

        AgentHelper.serializeJSON(profile);
    }

}
