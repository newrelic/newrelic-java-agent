/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EventTestHelper {

    public static String APP_NAME = "Unit Test";

    public static void setAppName(String appName) {
        APP_NAME = appName;
    }

    public static void createServiceManager(Map<String, Object> config) throws Exception {
        if (APP_NAME == null || APP_NAME.isEmpty()) {
            APP_NAME = "Unit Test";
        }
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config), config);
        serviceManager.setConfigService(configService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

        TransactionService txService = new TransactionService();
        serviceManager.setTransactionService(txService);

        TransactionTraceService ttService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(ttService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();

        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmServiceManager.setRPMService(rpmService);

        ErrorServiceImpl errorService = new ErrorServiceImpl(APP_NAME);
        rpmService.setErrorService(errorService);

        AttributesService attService = new AttributesService();
        serviceManager.setAttributesService(attService);

        serviceManager.setDistributedTraceService(new DistributedTraceServiceImpl());

        serviceManager.setRPMServiceManager(rpmServiceManager);
    }

    public static TransactionData generateTransactionData(String appName) {
        return generateTransactionData(Collections.<String, Object>emptyMap(), appName, 100L);
    }

    public static TransactionData generateTransactionDataAndComplete(Map<String, Object> userParams, String appName) {
        return generateTransactionData(userParams, appName, 100L);
    }

    public static TransactionData generateTransactionDataAndComplete(Map<String, Object> userParams, String appName, long durationInMillis) {
        return generateTransactionData(userParams, appName, durationInMillis);
    }

    private static TransactionData generateTransactionData(Map<String, Object> userParams, String appName, long durationInMillis) {
        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setWebTransaction(true);
        MockDispatcherTracer rootTracer = new MockDispatcherTracer();
        rootTracer.setDurationInMilliseconds(durationInMillis);
        long baseTime = System.nanoTime();
        rootTracer.setStartTime(baseTime);
        rootTracer.setEndTime(baseTime + TimeUnit.NANOSECONDS.convert(durationInMillis, TimeUnit.MILLISECONDS));

        // create a new string instance to verify that the transaction name cache is working
        String frontendMetricName = "Frontend/metricname" + System.currentTimeMillis();

        final AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(appName);

        return new TransactionDataTestBuilder(appName, agentConfig, rootTracer)
                .setDispatcher(rootTracer)
                .setFrontendMetricName(frontendMetricName)
                .setUserParams(userParams)
                .setIncludeDistributedTracePayload(true)
                .build();
    }
}
