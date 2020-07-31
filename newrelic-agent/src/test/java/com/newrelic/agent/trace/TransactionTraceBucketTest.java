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
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TransactionTraceBucketTest {

    private static final String APP_NAME = "Unit Test";

    @After
    public void teardown() throws Exception {
        ServiceManager manager = ServiceFactory.getServiceManager();
        if (manager != null) {
            manager.stop();
            ServiceFactory.setServiceManager(null);
        }
    }

    private AgentConfig createConfig(Map<String, Object> configMap) {
        return AgentConfigImpl.createAgentConfig(configMap);
    }

    private Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "staging-collector.newrelic.com");
        map.put("port", 80);
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        return map;
    }

    private void createServiceManager(Map<String, Object> configMap) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        AgentConfig agentConfig = createConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, configMap);
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

    @Test
    public void transactionThreshold() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttMap = createConfigMap();
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 1.0f);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        createServiceManager(configMap);

        ITransactionSampler transactionSampler = new TransactionTraceSampler();

        TransactionData td = createTransactionData("/en/betting/Football/*", 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

        td = createTransactionData("/en/betting/Football/*", 2000L);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(2000L, traces.get(0).getDuration());

    }

    @Test
    public void slowest() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttMap = createConfigMap();
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.5f);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        createServiceManager(configMap);

        ITransactionSampler transactionSampler = new TransactionTraceSampler();

        long durationInMillis = 1000L;
        TransactionData td = createTransactionData("/en/betting/Football/*", durationInMillis - 100);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        td = createTransactionData("/en/betting/Football/*", durationInMillis - 200);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());
    }

    @Test
    public void harvest() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttMap = createConfigMap();
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.5f);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        createServiceManager(configMap);

        ITransactionSampler transactionSampler = new TransactionTraceSampler();

        long durationInMillis = 1000L;
        TransactionData td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/de/betting/Chess/*", durationInMillis + 100L);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/de/betting/Chess/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis + 100L, traces.get(0).getDuration());

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());
    }

    @Test
    public void topN0() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttMap = createConfigMap();
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.5f);
        ttMap.put(TransactionTracerConfigImpl.TOP_N, 0);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        createServiceManager(configMap);

        ITransactionSampler transactionSampler = new TransactionTraceSampler();

        long durationInMillis = 1000L;
        TransactionData td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/en/betting/Football/*", durationInMillis - 100);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis - 100L, traces.get(0).getDuration());

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());
    }

    @Test
    public void topN1() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttMap = createConfigMap();
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.5f);
        ttMap.put(TransactionTracerConfigImpl.TOP_N, 1);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        createServiceManager(configMap);

        ITransactionSampler transactionSampler = new TransactionTraceSampler();

        long durationInMillis = 1000L;
        TransactionData td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/en/betting/Football/*", durationInMillis - 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        td = createTransactionData("/en/betting/Football/*", durationInMillis + 100L);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        td = createTransactionData("/en/betting/Football/*", durationInMillis + 200L);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis + 200L, traces.get(0).getDuration());

        td = createTransactionData("/en/betting/Football/*", durationInMillis - 100L);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

    }

    @Test
    public void topN2() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttMap = createConfigMap();
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.5f);
        ttMap.put(TransactionTracerConfigImpl.TOP_N, 2);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        createServiceManager(configMap);

        ITransactionSampler transactionSampler = new TransactionTraceSampler();

        long durationInMillis = 1000L;
        TransactionData td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        td = createTransactionData("/de/betting/Chess/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/de/betting/Chess/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        td = createTransactionData("/de/betting/Chess/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        td = createTransactionData("/ru/betting/Motorsport/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/ru/betting/Motorsport/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/en/betting/Football/*", durationInMillis - 200L);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        td = createTransactionData("/en/betting/Football/*", durationInMillis - 100L);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        td = createTransactionData("/ru/betting/Motorsport/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));

        td = createTransactionData("/ru/betting/Motorsport/*", durationInMillis + 100L);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/ru/betting/Motorsport/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis + 100L, traces.get(0).getDuration());

    }

    @Test
    public void noTraceCount() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttMap = createConfigMap();
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.5f);
        ttMap.put(TransactionTracerConfigImpl.TOP_N, 10);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        createServiceManager(configMap);

        ITransactionSampler transactionSampler = new TransactionTraceSampler();

        long durationInMillis = 1000L;
        TransactionData td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));

        List<TransactionTrace> traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));
        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));
        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));
        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));
        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertFalse(transactionSampler.noticeTransaction(td));
        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(0, traces.size());

        td = createTransactionData("/en/betting/Football/*", durationInMillis);
        Assert.assertTrue(transactionSampler.noticeTransaction(td));
        traces = transactionSampler.harvest(APP_NAME);
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Football/*", traces.get(0).getRequestUri());
        Assert.assertEquals(durationInMillis, traces.get(0).getDuration());

    }

}
