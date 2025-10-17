/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RandomTransactionSamplerzzzTest {

    private static final String APP_NAME = "Unit Test";

    @Before
    public void setup() throws Exception {
        createServiceManager();
    }

    @After
    public void teardown() throws Exception {
        ServiceFactory.getServiceManager().stop();
        ServiceFactory.setServiceManager(null);
    }

    private Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "staging-collector.newrelic.com");
        map.put("port", 80);
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        return map;
    }

    private void createServiceManager() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        Map<String, Object> configMap = createConfigMap();

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(configMap), configMap);
        serviceManager.setConfigService(configService);

        DatabaseService dbService = new DatabaseService();
        serviceManager.setDatabaseService(dbService);

        TransactionTraceService ttService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(ttService);

        AttributesService attService = new AttributesService();
        serviceManager.setAttributesService(attService);
    }

    private TransactionData createTransactionData(String transactionName, long durationInMillis) {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        MockDispatcher dispatcher = new MockDispatcher();
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

    @Test
    public void randomTransactionSamplerKeepsFirstTransactionInHarvest() {
        ITransactionSampler transactionSampler = new RandomTransactionSampler(5);

        long durationInMillis = 5000L;
        TransactionData td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis + 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());
    }

    @Test
    public void randomTransactionSamplerKeepsFirstTransactionDespiteVariations() throws Exception {
        ITransactionSampler transactionSampler = new RandomTransactionSampler(5);

        long durationInMillis = 10L;
        TransactionData td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis + 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/ru/betting/Motorsport/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis + 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/ru/betting/Motorsport/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

        td = createTransactionData("/ru/betting/Motorsport/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis + 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/ru/betting/Motorsport/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

        td = createTransactionData("/de/betting/Chess/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis + 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/de/betting/Chess/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/us/betting/baseball/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis + 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/us/betting/baseball/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/fr/betting/cycling/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis + 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/fr/betting/cycling/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

    }

    @Test
    public void samplerChecksForMatchingAppName() throws Exception {
        ITransactionSampler transactionSampler = new RandomTransactionSampler(5);

        long durationInMillis = 5000L;
        TransactionData td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis + 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest("Bogus");
        Assert.assertEquals(0, traces.size());

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());
    }

}
