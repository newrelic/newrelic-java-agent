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
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TransactionTraceServiceTest {

    private static final String APP_NAME = "Unit Test";
    private MockRPMServiceManager rpmServiceManager;

    @After
    public void teardown() throws Exception {
        ServiceFactory.getServiceManager().stop();
        ServiceFactory.setServiceManager(null);
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

        rpmServiceManager = new MockRPMServiceManager();
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

    private void removeRandomTTSamplers() {
        final TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        final List<ITransactionSampler> transactionSamplers = ttService.transactionSamplers;
        for (ITransactionSampler sampler : transactionSamplers) {
            if (sampler instanceof RandomTransactionSampler) {
                ttService.removeTransactionTraceSampler(sampler);
            }
        }
    }

    private Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "staging-collector.newrelic.com");
        map.put("port", 80);
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        return map;
    }

    // creates a non-synthetic transaction
    private TransactionData createTransactionData(String transactionName, String appName, long durationInMillis) {
        return createTransactionData(transactionName, appName, durationInMillis, null, null, null);
    }

    // if last three arguments are all non-null, created tx will appear to have come from New Relic Synthetics
    private TransactionData createTransactionData(String transactionName, String appName, long durationInMillis,
            String synJobId, String synMonitorId, String synResourceId) {
        MockDispatcher dispatcher = new MockDispatcher();
        MockDispatcherTracer rootTracer = new MockDispatcherTracer();
        rootTracer.setDurationInMilliseconds(durationInMillis);
        rootTracer.setStartTime(System.nanoTime());
        rootTracer.setEndTime(System.nanoTime() + TimeUnit.NANOSECONDS.convert(durationInMillis, TimeUnit.MILLISECONDS));

        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

        return new TransactionDataTestBuilder(appName, agentConfig, rootTracer)
                .setDispatcher(dispatcher)
                .setRequestUri(transactionName)
                .setFrontendMetricName(transactionName)
                .setSynJobId(synJobId)
                .setSynMonitorId(synMonitorId)
                .setSynResourceId(synResourceId)
                .build();
    }

    @Test
    public void threadCpuTimeEnabledFalse() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        configMap.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, false);

        createServiceManager(configMap);

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        Assert.assertFalse(ttService.isThreadCpuTimeEnabled());
    }

    @Test
    public void threadCpuTimeEnabledTrueNotIBM() throws Exception {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        boolean isEnabled = threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled();
        System.setProperty("java.vendor", "Apple Inc.");

        Map<String, Object> configMap = createConfigMap();
        configMap.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, true);

        createServiceManager(configMap);

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        Assert.assertEquals(isEnabled, ttService.isThreadCpuTimeEnabled());
    }

    @Test
    public void threadCpuTimeEnabledNullNotIBM() throws Exception {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        // boolean isEnabled = threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled();
        System.setProperty("java.vendor", "Apple Inc.");

        Map<String, Object> configMap = createConfigMap();

        createServiceManager(configMap);

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        Assert.assertEquals(false, ttService.isThreadCpuTimeEnabled());
    }

    @Test
    public void threadCpuTimeEnabledNullIBM() throws Exception {
        System.setProperty("java.vendor", "IBM Corporation");

        Map<String, Object> configMap = createConfigMap();

        createServiceManager(configMap);

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        Assert.assertFalse(ttService.isThreadCpuTimeEnabled());
    }

    @Test
    public void threadCpuTimeEnabledTrueIBM() throws Exception {
        System.setProperty("java.vendor", "IBM Corporation");

        Map<String, Object> configMap = createConfigMap();
        configMap.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, true);

        createServiceManager(configMap);

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        Assert.assertTrue(ttService.isThreadCpuTimeEnabled());
    }

    @Test
    public void appName() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttConfigMap = new HashMap<>();
        configMap.put(AgentConfigImpl.ENABLE_AUTO_APP_NAMING, true);
        configMap.put("transaction_tracer", ttConfigMap);
        ttConfigMap.put(TransactionTracerConfigImpl.ENABLED, true);
        ttConfigMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        ttConfigMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0L);

        createServiceManager(configMap);
        rpmServiceManager.getOrCreateRPMService("App Name 1");
        rpmServiceManager.getOrCreateRPMService("App Name 2");

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();

        TransactionData td = createTransactionData("/en/betting/Football/*", "App Name 1", 100L);
        ttService.dispatcherTransactionFinished(td, null);

        td = createTransactionData("/ru/betting/Motorsport/*", "App Name 2", 200L);
        ttService.dispatcherTransactionFinished(td, null);

        ttService.afterHarvest("App Name 1");
        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMServiceManager().getRPMService("App Name 1");
        Assert.assertEquals(1, rpmService.getTraces().size());
        Assert.assertEquals("/en/betting/Football/*", rpmService.getTraces().get(0).getRequestUri());
        Assert.assertEquals(100L, rpmService.getTraces().get(0).getDuration());

        ttService.afterHarvest("App Name 2");
        rpmService = (MockRPMService) ServiceFactory.getRPMServiceManager().getRPMService("App Name 2");
        Assert.assertEquals(1, rpmService.getTraces().size());
        Assert.assertEquals("/ru/betting/Motorsport/*", rpmService.getTraces().get(0).getRequestUri());
        Assert.assertEquals(200L, rpmService.getTraces().get(0).getDuration());
    }

    @Test
    public void transactionSamplers() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttConfigMap = new HashMap<>();
        configMap.put("transaction_tracer", ttConfigMap);
        ttConfigMap.put(TransactionTracerConfigImpl.ENABLED, true);
        ttConfigMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);

        createServiceManager(configMap);

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        ITransactionSampler transactionSampler = new ITransactionSampler() {

            private final List<TransactionData> tds = new ArrayList<>();

            @Override
            public boolean noticeTransaction(TransactionData td) {
                tds.add(td);
                return true;
            }

            @Override
            public List<TransactionTrace> harvest(String appName) {
                List<TransactionTrace> traces = new ArrayList<>();
                for (TransactionData td : tds) {
                    TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
                    traces.add(trace);
                }
                return traces;
            }

            @Override
            public void stop() {
                tds.clear();
            }

        };
        ITransactionSampler transactionSampler2 = new ITransactionSampler() {

            private final List<TransactionData> tds = new ArrayList<>();

            @Override
            public boolean noticeTransaction(TransactionData td) {
                tds.add(td);
                return true;
            }

            @Override
            public List<TransactionTrace> harvest(String appName) {
                List<TransactionTrace> traces = new ArrayList<>();
                for (TransactionData td : tds) {
                    TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
                    traces.add(trace);
                }
                return traces;
            }

            @Override
            public void stop() {
                tds.clear();
            }

        };
        ttService.addTransactionTraceSampler(transactionSampler);
        ttService.addTransactionTraceSampler(transactionSampler2);

        TransactionData td = createTransactionData("/en/betting/Baseball/*", APP_NAME, 100L);
        ttService.dispatcherTransactionFinished(td, null);
        ttService.afterHarvest(APP_NAME);

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMServiceManager().getOrCreateRPMService(APP_NAME);
        List<TransactionTrace> traces = rpmService.getTraces();
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Baseball/*", traces.get(0).getRequestUri());
        Assert.assertEquals(100L, traces.get(0).getDuration());
        traces.clear();
        traces = rpmService.getTraces();
        Assert.assertEquals(0, traces.size());

        ttService.removeTransactionTraceSampler(transactionSampler);
        ttService.removeTransactionTraceSampler(transactionSampler2);
        ttService.dispatcherTransactionFinished(td, null);
        ttService.afterHarvest(APP_NAME);
        Assert.assertEquals(0, transactionSampler.harvest(APP_NAME).size());
        Assert.assertEquals(0, transactionSampler2.harvest(APP_NAME).size());
    }

    @Test
    public void randomTransactionSampler() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttConfigMap = new HashMap<>();
        configMap.put("transaction_tracer", ttConfigMap);
        ttConfigMap.put(TransactionTracerConfigImpl.ENABLED, true);
        ttConfigMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        ttConfigMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 1000f);

        createServiceManager(configMap);

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        TransactionData td = createTransactionData("/en/betting/Baseball/*", APP_NAME, 10L);
        ttService.dispatcherTransactionFinished(td, null);
        ttService.afterHarvest(APP_NAME);

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMServiceManager().getOrCreateRPMService(APP_NAME);
        List<TransactionTrace> traces = rpmService.getTraces();
        Assert.assertEquals(1, traces.size());
        Assert.assertEquals("/en/betting/Baseball/*", traces.get(0).getRequestUri());
        Assert.assertEquals(10L, traces.get(0).getDuration());
    }

    @Test
    public void transactionSamplersWithSynthetics() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttConfigMap = new HashMap<>();
        configMap.put("transaction_tracer", ttConfigMap);
        ttConfigMap.put(TransactionTracerConfigImpl.ENABLED, true);
        ttConfigMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        ttConfigMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 1000f);

        createServiceManager(configMap);

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();

        // This sampler only saves traces with "baseball" in the tx name
        ITransactionSampler transactionSampler = new ITransactionSampler() {

            private final List<TransactionData> tds = new ArrayList<>();

            @Override
            public boolean noticeTransaction(TransactionData td) {
                if (td.getPriorityTransactionName().getName().contains("baseball")) {
                    tds.add(td);
                    return true;
                }
                return false;
            }

            @Override
            public List<TransactionTrace> harvest(String appName) {
                List<TransactionTrace> traces = new ArrayList<>();
                for (TransactionData td : tds) {
                    TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
                    traces.add(trace);
                }
                return traces;
            }

            @Override
            public void stop() {
                tds.clear();
            }

        };

        // This sampler only saves traces with "football" in the tx name
        ITransactionSampler transactionSampler2 = new ITransactionSampler() {

            private final List<TransactionData> tds = new ArrayList<>();

            @Override
            public boolean noticeTransaction(TransactionData td) { // football tx's only
                if (td.getPriorityTransactionName().getName().contains("football")) {
                    tds.add(td);
                    return true;
                }
                return false;
            }

            @Override
            public List<TransactionTrace> harvest(String appName) {
                List<TransactionTrace> traces = new ArrayList<>();
                for (TransactionData td : tds) {
                    TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
                    traces.add(trace);
                }
                return traces;
            }

            @Override
            public void stop() {
                tds.clear();
            }

        };

        // This sampler saves the longest trace it sees
        ITransactionSampler transactionSampler3 = new ITransactionSampler() {

            private final List<TransactionData> tds = new ArrayList<>();

            @Override
            public boolean noticeTransaction(TransactionData td) {
                if (tds.size() == 0) {
                    tds.add(td);
                    return true;
                } else if (td.getLegacyDuration() > tds.get(0).getLegacyDuration()) {
                    tds.clear();
                    tds.add(td);
                }
                return false;
            }

            @Override
            public List<TransactionTrace> harvest(String appName) {
                List<TransactionTrace> traces = new ArrayList<>();
                for (TransactionData td : tds) {
                    TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
                    traces.add(trace);
                }
                return traces;
            }

            @Override
            public void stop() {
                tds.clear();
            }

        };

        ttService.addTransactionTraceSampler(transactionSampler);
        ttService.addTransactionTraceSampler(transactionSampler2);
        ttService.addTransactionTraceSampler(transactionSampler3);

        addTx("haggis-eating", 100L, false);
        addTx("baseball", 100L, false);
        addTx("baseball", 101L, true);
        addTx("football", 100L, false);
        addTx("football", 101L, true);
        addTx("cricket", 100L, false);
        addTx("cricket", 101L, true);
        addTx("curling", 1500L, false);
        addTx("curling", 2501L, true);

        ttService.afterHarvest(APP_NAME);

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMServiceManager().getOrCreateRPMService(APP_NAME);
        List<TransactionTrace> traces = rpmService.getTraces();

        System.out.println(traces);
        Assert.assertEquals(8, traces.size());

        // The synthetics sampler should have picked up all the traces with lengths ending in a 1
        int nSynthetic = 0;
        for (TransactionTrace tt : traces) {
            if (tt.getDuration() % 10 == 1) {
                nSynthetic++;
            }
        }
        Assert.assertEquals(4, nSynthetic);

        // And other samplers should have captured everything but (cricket, 100)
        for (TransactionTrace tt : traces) {
            if (tt.getRequestUri().contains("cricket") && tt.getDuration() == 100L) {
                Assert.fail("samplers not behaving as expected.");
            }
        }

        traces.clear();
        traces = rpmService.getTraces();
        Assert.assertEquals(0, traces.size());
    }

    private void addTx(String sport, long duration, boolean synthetic) {
        TransactionData td;
        if (synthetic) {
            td = createTransactionData("/en/betting/" + sport + "/*", APP_NAME, duration, "job", "monitor", "resource");
        } else {
            td = createTransactionData("/en/betting/" + sport + "/*", APP_NAME, duration);
        }
        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        ttService.dispatcherTransactionFinished(td, null);
    }

    @Test
    public void testKeyTransactionSampler() throws Exception {
        final String keyTransactionName = "/a/key/transaction";
        final String normalTransactionName = "/a/normal/transaction";
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> ttConfigMap = new HashMap<>();
        configMap.put(AgentConfigImpl.ENABLE_AUTO_APP_NAMING, false);
        configMap.put("transaction_tracer", ttConfigMap);
        ttConfigMap.put(TransactionTracerConfigImpl.ENABLED, true);
        ttConfigMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        ttConfigMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 100L);
        Map<String, Object> ktConfig = new HashMap<>();
        ktConfig.put(keyTransactionName, 0.04);
        configMap.put(AgentConfigImpl.KEY_TRANSACTIONS, ktConfig);

        createServiceManager(configMap);
        removeRandomTTSamplers();

        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();

        TransactionData td = createTransactionData(keyTransactionName, "App Name", 50);
        rpmServiceManager.getOrCreateRPMService("App Name");
        Assert.assertTrue(td.getAgentConfig().isApdexTSet(keyTransactionName));
        Assert.assertEquals(40, td.getAgentConfig().getApdexTInMillis(keyTransactionName));
        ttService.dispatcherTransactionFinished(td, null);

        TransactionData td2 = createTransactionData(normalTransactionName, "App Name", 50);
        Assert.assertFalse(td2.getAgentConfig().isApdexTSet(normalTransactionName));
        ttService.dispatcherTransactionFinished(td2, null);

        ttService.afterHarvest("App Name");
        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMServiceManager().getRPMService("App Name");
        Assert.assertEquals(1, rpmService.getTraces().size());
        Assert.assertEquals(keyTransactionName, rpmService.getTraces().get(0).getRequestUri());
        Assert.assertEquals(50L, rpmService.getTraces().get(0).getDuration());
    }
}
