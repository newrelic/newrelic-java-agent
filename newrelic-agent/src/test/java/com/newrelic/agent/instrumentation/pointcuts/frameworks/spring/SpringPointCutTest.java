/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.util.Map;

import org.junit.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigFactoryTest;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.service.ServiceFactory;

public class SpringPointCutTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        Map<String, Object> settings = AgentConfigFactoryTest.createStagingMap();
        AgentConfig config = AgentConfigFactory.createAgentConfig(settings, null, null);
        MockConfigService configService = new MockConfigService(null);
        configService.setAgentConfig(config);
        serviceManager.setConfigService(configService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName("Unit Test");
        rpmServiceManager.setRPMService(rpmService);

        serviceManager.setNormalizationService(new NormalizationServiceImpl());
    }

    @Test
    public void nullView() {
        Assert.assertEquals(null, SpringPointCut.cleanModelAndViewName(null));
    }

    @Test
    public void emptyView() {
        Assert.assertEquals("", SpringPointCut.cleanModelAndViewName(""));
    }

    @Test
    public void shortView() {
        Assert.assertEquals("/a", SpringPointCut.cleanModelAndViewName("a"));
    }

    @Test
    public void noLeadingSlash() {
        Assert.assertEquals("/fooDude", SpringPointCut.cleanModelAndViewName("fooDude"));
    }

    @Test
    public void withLeadingSlash() {
        Assert.assertEquals("/fooDude", SpringPointCut.cleanModelAndViewName("/fooDude"));
    }

    @Test
    public void parametersInView() {
        Assert.assertEquals("/foo", SpringPointCut.cleanModelAndViewName("/foo?man=chu"));
        Assert.assertEquals("/foo", SpringPointCut.cleanModelAndViewName("/foo?man=chu&second=jfjfj"));

        Assert.assertEquals("/foo", SpringPointCut.cleanModelAndViewName("/foo#duuuude"));
        Assert.assertEquals("/foo", SpringPointCut.cleanModelAndViewName("/foo;jsessionid=082377467"));
    }

    @Test
    public void httpInString() {
        Assert.assertEquals("/dude:*", SpringPointCut.cleanModelAndViewName("dude:http://foo"));
        Assert.assertEquals("/*", SpringPointCut.cleanModelAndViewName("/http://hey"));
    }

    @Test
    public void httpsInString() {
        Assert.assertEquals("/testUrlInViewName:*",
                SpringPointCut.cleanModelAndViewName("/testUrlInViewName:https://foo"));
    }

    @Test
    public void redirectView() {
        Assert.assertEquals("/redirect:*", SpringPointCut.cleanModelAndViewName("redirect:/foo"));

        Assert.assertEquals("/redirect:*", SpringPointCut.cleanModelAndViewName("/redirect:/foo"));
        Assert.assertEquals("/redirect:*", SpringPointCut.cleanModelAndViewName("redirect:foo"));

        Assert.assertEquals(
                "/redirect:*",
                SpringPointCut.cleanModelAndViewName("/redirect:https://www.example.com/signin/viewSignin.html?fid=83d44a98c9964ef"));
    }

    @Test
    public void forwardView() {
        Assert.assertEquals(null, SpringPointCut.cleanModelAndViewName("forward:/foo"));

        Assert.assertEquals(null, SpringPointCut.cleanModelAndViewName("/forward:/foo"));
        Assert.assertEquals(null, SpringPointCut.cleanModelAndViewName("forward:foo"));

    }
}
