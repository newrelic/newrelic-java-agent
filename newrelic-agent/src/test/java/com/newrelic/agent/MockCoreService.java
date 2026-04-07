/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.ServiceManagerImpl;
import com.newrelic.agent.agentcontrol.HealthDataChangeListener;
import com.newrelic.agent.agentcontrol.HealthDataProducer;
import com.newrelic.api.agent.Logger;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

public class MockCoreService extends AbstractService implements CoreService, HealthDataProducer {
    private InstrumentationProxy instrumentation = null;

    public MockCoreService() {
        super(CoreService.class.getSimpleName());
    }

    public static CoreService getMockAgentAndBootstrapTheServiceManager() throws Exception {
        AgentHelper.initializeConfig();
        MockCoreService mockCoreService = new MockCoreService();
        mockCoreService.setInstrumentation(Mockito.mock(InstrumentationProxy.class));
        Mockito.when(mockCoreService.getInstrumentation().getAllLoadedClasses()).thenReturn(new Class[] {});

        ConfigService configService = ConfigServiceFactory.createConfigService(mock(Logger.class), false);
        ServiceManager serviceManager = new ServiceManagerImpl(mockCoreService, configService);
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();
        return mockCoreService;
    }

    private volatile int shutdownCount;

    public int getShutdownCount() {
        return shutdownCount;
    }

    @Override
    public InstrumentationProxy getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(InstrumentationProxy instrumentationProxy) {
        instrumentation = instrumentationProxy;
    }

    @Override
    public void shutdownAsync() {
        shutdownCount++;
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

    @Override
    public void registerHealthDataChangeListener(HealthDataChangeListener listener) {
    }
}
