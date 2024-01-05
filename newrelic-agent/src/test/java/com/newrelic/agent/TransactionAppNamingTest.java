/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.test.marker.RequiresFork;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

@Category(RequiresFork.class)
public class TransactionAppNamingTest {

    private static final String APP_NAME = "TransactionAppNamingTest";

    private Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        map.put("apdex_t", 0.5f);
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return map;
    }

    @Before
    public void createServiceManager() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        // Needed by TransactionService
        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        // Needed by TransactionTraceService
        Map<String, Object> configMap = createConfigMap();

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);
        serviceManager.setConfigService(configService);

        // Needed by Transaction
        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);
        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        // Null pointers if not set
        serviceManager.setStatsService(Mockito.mock(StatsService.class));

        // Needed by Transaction
        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        serviceManager.setAttributesService(new AttributesService());

        // Needed by Transaction
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmServiceManager.setRPMService(rpmService);
    }
    
    @Test
    public void testGetConfiguredDefault() {
        Transaction tx = Transaction.getTransaction();
        Assert.assertEquals(ApplicationNamePriority.NONE, tx.getPriorityApplicationName().getPriority());
        Assert.assertEquals(APP_NAME, tx.getPriorityApplicationName().getName());
        Assert.assertEquals(1, tx.getPriorityApplicationName().getNames().size());
        Assert.assertEquals(tx.getPriorityApplicationName().getNames().get(0), tx.getPriorityApplicationName().getName());
        Assert.assertEquals(tx.getPriorityApplicationName().getName(), tx.getApplicationName());
    }

    @Test
    public void testSetAndThenGet() {
        Transaction tx = Transaction.getTransaction();
        tx.setApplicationName(ApplicationNamePriority.SERVLET_INIT_PARAM, "ServletName");
        Assert.assertEquals(ApplicationNamePriority.SERVLET_INIT_PARAM, tx.getPriorityApplicationName().getPriority());
        Assert.assertEquals("ServletName", tx.getPriorityApplicationName().getName());
        Assert.assertEquals(1, tx.getPriorityApplicationName().getNames().size());
        Assert.assertEquals(tx.getPriorityApplicationName().getNames().get(0), tx.getPriorityApplicationName().getName());
        Assert.assertEquals(tx.getPriorityApplicationName().getName(), tx.getApplicationName());
    }

}
