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
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.api.agent.ApplicationNamePriority;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class CalculatePathHashTest {

    private static final String APP_NAME = "CalculateHashPathTest";

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

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(
                configMap), configMap);
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
    public void testCalculatePathHash() {
        Transaction tx = Transaction.getTransaction();
        String pathHash = ServiceUtils.intToHexString(ServiceUtils.calculatePathHash(APP_NAME,
                tx.getPriorityTransactionName().getName(), tx.getInboundHeaderState().getReferringPathHash()));
        CrossProcessTransactionStateImpl crossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);

        Assert.assertEquals(pathHash, ServiceUtils.intToHexString(crossProcessTransactionState.generatePathHash()));
        Assert.assertEquals(APP_NAME, tx.getPriorityApplicationName().getName());
        Assert.assertEquals(1, tx.getPriorityApplicationName().getNames().size());
        Assert.assertEquals(tx.getPriorityApplicationName().getNames().get(0),
                tx.getPriorityApplicationName().getName());
        Assert.assertEquals(tx.getPriorityApplicationName().getName(), tx.getApplicationName());

        // Now change the transaction application's name
        tx.setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE, "TestingCalculatePathHash;CalculatePathHash");
        String newPathHash = ServiceUtils.intToHexString(ServiceUtils.calculatePathHash("TestingCalculatePathHash",
                tx.getPriorityTransactionName().getName(), tx.getInboundHeaderState().getReferringPathHash()));
        CrossProcessTransactionStateImpl newCrossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);
        Assert.assertEquals(newPathHash, ServiceUtils.intToHexString(
                newCrossProcessTransactionState.generatePathHash()));
        Assert.assertEquals(2, tx.getPriorityApplicationName().getNames().size());
        Assert.assertEquals(tx.getPriorityApplicationName().getNames().get(0),
                tx.getPriorityApplicationName().getName());
        Assert.assertEquals(tx.getPriorityApplicationName().getNames().get(1), "CalculatePathHash");
    }

}
