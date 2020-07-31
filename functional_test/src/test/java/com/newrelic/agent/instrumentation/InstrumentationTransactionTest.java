/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.InsightsService;
import com.newrelic.api.agent.Trace;

public class InstrumentationTransactionTest {

    @Test
    public void testExceptionInTransactionInitialization() {
        ServiceManager serviceManager = Mockito.spy(ServiceFactory.getServiceManager());
        InsightsService insightsService = Mockito.spy(serviceManager.getInsights());
        Mockito.when(serviceManager.getInsights()).thenReturn(insightsService);

        // Force throw an exception in the Transaction constructor
        Mockito.doThrow(new RuntimeException()).when(insightsService).getTransactionInsights(
                Mockito.any(AgentConfig.class));

        ServiceFactory.setServiceManager(serviceManager);

        // Tracing the current test method doesn't allow us to use the
        // mocked RuntimeException so we'll put it on a dummy method instead
        doNothing();
    }

    @Trace(dispatcher = true)
    public int doNothing() {
        return 0;
    }
}
