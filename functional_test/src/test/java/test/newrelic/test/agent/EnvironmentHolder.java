/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.ConfigServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import test.newrelic.EnvironmentHolderSettingsGenerator;

import java.util.Map;

// Functional tests run in standalone processes, which means the Agent has already initialized itself normally
// before the test starts running. But here in the test, we want to load a specific configuration which sits
// statically in the newrelic-agent/src/test/resources/com/newrelic/agent/config/newrelic.yml file as the
// environment named "expected_errors_test" (in our yml, an environment is more commonly e.g. "dev", "staging",
// "production", etc. - it's a way of specifying a set of configuration overrides for a particular setting. Here,
// we are using it for a particular test, this one).
//
// The Agent is not really designed to have configuration reloaded or having the service manager replaced on the
// fly or similar things, so the code is really fussy. It is all encapsulated here. Create one of these to save
// off the service manager and load a new config; close() it to put things back. It would be nice to support
// AutoCloseable, but that would prevent us from running functional tests on Java 6.

public class EnvironmentHolder {

    private static class StatsEngineCollector implements MockRPMService.MockHarvestListener {
        private StatsEngine statsEngine;

        @Override
        public void mockHarvest(StatsEngine statsEngine) {
            if (statsEngine == null) {
                return;
            }
            if (this.statsEngine != null) {
                this.statsEngine.mergeStats(statsEngine);
                return;
            }
            this.statsEngine = statsEngine;
        }

        public StatsEngine getStatsEngine() {
            return statsEngine;
        }
    }

    private TransactionDataList transactionList = new TransactionDataList();
    private AgentConfig config;

    private final MockRPMService rpmService;
    private static final ServiceManager originalServiceManager = ServiceFactory.getServiceManager();
    private final EnvironmentHolderSettingsGenerator environmentHolderSettingsGenerator;
    private final StatsEngineCollector statsEngineCollector;

    public EnvironmentHolder(EnvironmentHolderSettingsGenerator environmentHolderSettingsGenerator) {
        this.environmentHolderSettingsGenerator = environmentHolderSettingsGenerator;
        statsEngineCollector = new StatsEngineCollector();
        rpmService = new MockRPMService(null, statsEngineCollector);
    }

    public void setupEnvironment() throws Exception {
        rpmService.setIsConnected(true);
        IRPMService originalRpmService = originalServiceManager.getRPMServiceManager().getRPMService();
        rpmService.setApplicationName(originalRpmService.getApplicationName());

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(environmentHolderSettingsGenerator.generateSettings());
        Map<String, Object> properties = ImmutableMap.<String, Object>of(
                "agent_config", ((BaseConfig) agentConfig).getProperties(),
                "collect_span_events", true);
        ((ConfigServiceImpl) ServiceFactory.getConfigService()).connected(rpmService, properties);
        if (agentConfig.getDistributedTracingConfig().isEnabled()) {
            ((DistributedTraceServiceImpl) ServiceFactory.getDistributedTraceService()).connected(rpmService, agentConfig);
        }
        config = agentConfig;

        // Set up now that new environment is in place
        ServiceFactory.getTransactionService().addTransactionListener(transactionList);
        transactionList.clear();

        // We have to actually start the harvest service, even though typically tests call harvestNow(). This means
        // the test will conflict with a real harvest if it takes more than about 30 seconds, possibly leading to
        // flickers. We should probably change the design of the harvest service. Better yet, we should implement a
        // better way to have custom configurations in functional tests and get rid of this messy class completely.
        ServiceFactory.getHarvestService().startHarvest(rpmService);
    }

    public TransactionDataList getTransactionList() {
        return transactionList;
    }
    
    public StatsEngine getStatsEngine() {
        return statsEngineCollector.getStatsEngine();
    }

    public void close() {
        transactionList.clear();
        ServiceFactory.getTransactionService().removeTransactionListener(transactionList);
        Transaction.clearTransaction();
        if (getStatsEngine() != null) {
            getStatsEngine().clear();
        }
        ServiceFactory.getHarvestService().stopHarvest(rpmService);
    }

}
