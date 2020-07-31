/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.core.CoreService;
import com.newrelic.api.agent.Logger;
import org.junit.Assert;
import org.junit.Test;

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
