/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.commands.CommandParser;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeyTransactionTraceSamplerzzzTest {

    private static final String APP_NAME = "Unit Test";

    @Test
    public void testNoticeKeyTransactionTrace() throws Exception {
        Map<String, Double> keyTransactions = new HashMap<>();
        final double transactionThresholdInSeconds = 0.5;
        keyTransactions.put("myKeyTxn", transactionThresholdInSeconds);
        setupAgent(keyTransactions);

        KeyTransactionTraceSampler sampler = new KeyTransactionTraceSampler();

        // Exceeds threshold
        final long keyTxnDurationInSec = 1000;
        final String keyTxnName = "myKeyTxn";
        sampler.noticeTransaction(createTransactionData(keyTxnName, keyTxnDurationInSec));

        List<TransactionTrace> traces = sampler.harvest(APP_NAME);
        assertFalse(traces.isEmpty());
        assertEquals(keyTxnDurationInSec, traces.get(0).getDuration());
        assertEquals(keyTxnName, traces.get(0).getRootMetricName());
    }


    @Test
    public void testKeyTxnNoTrace() throws Exception {
        Map<String, Double> keyTransactions = new HashMap<>();
        final double transactionThresholdInSeconds = 0.5;
        keyTransactions.put("myKeyTxn", transactionThresholdInSeconds);
        setupAgent(keyTransactions);

        KeyTransactionTraceSampler sampler = new KeyTransactionTraceSampler();

        // Doesn't exceed threshold
        sampler.noticeTransaction(createTransactionData("myKeyTxn", 100));
        List<TransactionTrace> traces = sampler.harvest(APP_NAME);
        assertTrue(traces.isEmpty());
    }

    @Test
    public void testNotAKeyTransaction() throws Exception {
        setupAgent(Collections.<String, Double>emptyMap());
        KeyTransactionTraceSampler sampler = new KeyTransactionTraceSampler();
        sampler.noticeTransaction(createTransactionData("notAKeyTxn", 1000));
        assertTrue(sampler.harvest(APP_NAME).isEmpty());
    }

    @Test
    public void testHigher() throws Exception {
        Map<String, Double> keyTransactions = new HashMap<>();
        keyTransactions.put("keyTxnOne", 0.5);
        keyTransactions.put("keyTxnTwo", 0.25);
        setupAgent(keyTransactions);

        KeyTransactionTraceSampler sampler = new KeyTransactionTraceSampler();

        TransactionData keyTxnOne = createTransactionData("keyTxnOne", 1000);
        TransactionData keyTxnTwo = createTransactionData("keyTxnTwo", 750);
        sampler.noticeTransaction(keyTxnOne);
        sampler.noticeTransaction(keyTxnTwo);

        // keyTxnTwo should be the most expensive transaction (it is 3x it's apdex).
        assertTrue(sampler.getScore(keyTxnTwo) > sampler.getScore(keyTxnOne));

        List<TransactionTrace> traces = sampler.harvest(APP_NAME);
        TransactionTrace transactionTrace = traces.get(0);
        assertEquals("keyTxnTwo", transactionTrace.getRequestUri());
    }

    @Test
    public void testSameScore() throws Exception {
        Map<String, Double> keyTransactions = new HashMap<>();
        keyTransactions.put("keyTxnOne", 0.5);
        keyTransactions.put("keyTxnTwo", 0.25);
        setupAgent(keyTransactions);

        KeyTransactionTraceSampler sampler = new KeyTransactionTraceSampler();
        TransactionData keyTxnOne = createTransactionData("keyTxnOne", 1000);
        TransactionData keyTxnTwo = createTransactionData("keyTxnTwo", 500);

        sampler.noticeTransaction(keyTxnOne);
        sampler.noticeTransaction(keyTxnTwo);

        assertEquals(sampler.getScore(keyTxnOne), sampler.getScore(keyTxnTwo));

        // First one noticed should win
        List<TransactionTrace> traces = sampler.harvest(APP_NAME);
        TransactionTrace transactionTrace = traces.get(0);
        assertEquals("keyTxnOne", transactionTrace.getRequestUri());


        sampler.noticeTransaction(keyTxnTwo);
        sampler.noticeTransaction(keyTxnOne);
        transactionTrace = sampler.harvest(APP_NAME).get(0);
        assertEquals("keyTxnTwo", transactionTrace.getRequestUri());
    }

    @Test
    public void testInvalidApdex() throws Exception {
        /**
         * The UI accepts a number like 0.0000000000000001 seconds for a key transaction apdex.
         * The collector transmits that to the agent, and the agent converts to millis and stores it in a long.
         * What could go wrong?
         *
         * Let's make sure the score calculation doesn't divide by zero.
         */

        Map<String, Double> keyTransactions = new HashMap<>();
        final double transactionThresholdInSeconds = 0.0000000000000001;
        keyTransactions.put("myKeyTxn", transactionThresholdInSeconds);
        setupAgent(keyTransactions);
        KeyTransactionTraceSampler sampler = new KeyTransactionTraceSampler();
        sampler.getScore(createTransactionData("myKeyTxn", 1000));
    }

    private void setupAgent(Map<String, Double> keyTransactions) throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("host", "staging-collector.newrelic.com");
        config.put("port", 80);
        config.put(AgentConfigImpl.APP_NAME, APP_NAME);
        config.put(AgentConfigImpl.KEY_TRANSACTIONS, keyTransactions);

        createServiceManager(config);
    }

    private void createServiceManager(Map<String, Object> configMap) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);
        serviceManager.setConfigService(configService);

        DatabaseService dbService = new DatabaseService();
        serviceManager.setDatabaseService(dbService);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setIsConnected(true);
        rpmServiceManager.setRPMService(rpmService);

        CommandParser commandParser = new CommandParser();
        serviceManager.setCommandParser(commandParser);

        AttributesService attService = new AttributesService();
        serviceManager.setAttributesService(attService);

        TransactionTraceService ttService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(ttService);
        ttService.start();
    }

    private TransactionData createTransactionData(String transactionName, long durationInMillis) {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setWebTransaction(true);
        MockDispatcherTracer rootTracer = new MockDispatcherTracer();
        rootTracer.setDurationInMilliseconds(durationInMillis);
        rootTracer.setStartTime(System.nanoTime());
        rootTracer.setEndTime(rootTracer.getStartTime()
                + TimeUnit.NANOSECONDS.convert(durationInMillis, TimeUnit.MILLISECONDS));

        return new TransactionDataTestBuilder(APP_NAME, agentConfig, rootTracer)
                .setDispatcher(dispatcher)
                .setRequestUri(transactionName)
                .setFrontendMetricName(transactionName)
                .build();
    }

}
