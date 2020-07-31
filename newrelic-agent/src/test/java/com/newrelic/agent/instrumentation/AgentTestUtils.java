/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import org.junit.Assert;

import java.text.MessageFormat;
import java.util.Map;

public class AgentTestUtils {

    public static MockServiceManager createServiceManager(Map<String, Object> configMap) throws Exception {
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

        DatabaseService dbService = new DatabaseService();
        serviceManager.setDatabaseService(dbService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName("name");
        rpmService.setEverConnected(true);
        rpmService.setErrorService(new ErrorServiceImpl("name"));
        rpmServiceManager.setRPMService(rpmService);

        configService.start();
        serviceManager.start();
        sqlTraceService.start();

        return serviceManager;
    }

    public static void assertVariance(long expected, long actual, double tolerance) {
        double variance = Math.abs(expected - actual) / (double) expected;
        Assert.assertTrue(MessageFormat.format("{1} is not within {2}% of {0} (was {3}).", expected, actual, tolerance,
                variance), variance <= tolerance);
    }
}
