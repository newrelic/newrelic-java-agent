/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.Agent;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.CrossProcessTransactionState;
import com.newrelic.agent.DistributedTracePayloadBuilder;
import com.newrelic.agent.DistributedTracingConfigTestUtil.DTConfigMapBuilder;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.tracing.samplers.AdaptiveSampler;
import com.newrelic.agent.tracing.samplers.Sampler;
import com.newrelic.agent.tracing.samplers.SamplerFactory;
import com.newrelic.api.agent.TransportType;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.SpanEventsConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventBuilder;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionTimer;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.NewRelic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase.REMOTE_PARENT_NOT_SAMPLED;
import static com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase.REMOTE_PARENT_SAMPLED;
import static com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase.ROOT;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class DistributedTraceServiceImplTest {

    private static DistributedTraceServiceImpl distributedTraceService;
    private static MockServiceManager serviceManager;
    private static MockRPMServiceManager rpmServiceManager;

    @Before
    public void before() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, "Test");

        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put("enabled", true);
        config.put("distributed_tracing", dtConfig);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        config.put("span_events", spanConfig);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config),
                Collections.<String, Object>emptyMap());

        serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        serviceManager.setConfigService(configService);
        serviceManager.setTransactionTraceService(new TransactionTraceService());
        serviceManager.setTransactionService(new TransactionService());

        distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));

        serviceManager.setHarvestService(new HarvestServiceImpl());
        serviceManager.setStatsService(new StatsServiceImpl());
        rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        ServiceFactory.getServiceManager().start();
    }

    @Test
    public void txnFinished() {
        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        TransactionStats transactionStats = new TransactionStats();

        Map<String, Object> intrinsicAttributes = new HashMap<>();
        long startTimeInMillis = System.currentTimeMillis();
        long responseTimeInNanos = TimeUnit.MILLISECONDS.toNanos(1350);

        TransactionData transactionData = createTransactionData(intrinsicAttributes, startTimeInMillis, responseTimeInNanos,
                null);
        distributedTraceService.dispatcherTransactionFinished(transactionData, transactionStats);

        SimpleStatsEngine unscopedStats = transactionStats.getUnscopedStats();
        String responseTime = MessageFormat.format(MetricNames.DURATION_BY_PARENT_UNKNOWN_ALL, "HTTPS");
        String errors = MessageFormat.format(MetricNames.ERRORS_BY_PARENT_UNKNOWN, "HTTPS");

        assertEquals(1.35, unscopedStats.getOrCreateResponseTimeStats(responseTime).getTotal(), 0.01f);
        assertEquals(1, unscopedStats.getStats(errors).getCallCount());
    }

    @Test
    public void txnFinishedInboundPayload() {
        Map<String, Object> connectInfo = new HashMap<>();
        connectInfo.put(DistributedTracingConfig.ACCOUNT_ID, "1acct234");
        connectInfo.put(DistributedTracingConfig.TRUSTED_ACCOUNT_KEY, "67890");
        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, Collections.<String, Object>emptyMap(), connectInfo);
        distributedTraceService.connected(null, agentConfig);
        TransactionStats transactionStats = new TransactionStats();

        Map<String, Object> intrinsicAttributes = new HashMap<>();

        long payloadSendTimestamp = 1002000;
        long startTimeInMillis = 1005000;
        long responseTimeInNanos = TimeUnit.MILLISECONDS.toNanos(1350);

        String json = new DistributedTracePayloadBuilder().setTimestamp(payloadSendTimestamp)
                .setHost("datanerd.us.com")
                .setParentType("Browser")
                .setAccountId("123456")
                .setTrustKey("67890")
                .setApplicationId("6789")
                .setTransactionId("badcafe3")
                .setTripId("cattrip")
                .setPriority(0.0f)
                .setDepth(0)
                .setSyntheticsJob(null)
                .setSyntheticsMonitor(null)
                .setSyntheticsResource(null)
                .createJsonPayload();

        DistributedTracePayloadImpl payload = new DistributedTracePayloadParser(NewRelic.getAgent().getMetricAggregator(), distributedTraceService,
            Agent.LOG).parse(null, json);
        TransactionData transactionData = createTransactionData(intrinsicAttributes, startTimeInMillis,
                responseTimeInNanos, payload);
        distributedTraceService.dispatcherTransactionFinished(transactionData, transactionStats);

        SimpleStatsEngine unscopedStats = transactionStats.getUnscopedStats();
        assertEquals(1.35f, unscopedStats.getOrCreateResponseTimeStats("DurationByCaller/Browser/123456/6789/HTTPS/all").getTotal(), 0.01f);
        assertEquals(3.0f, unscopedStats.getOrCreateResponseTimeStats("TransportDuration/Browser/123456/6789/HTTPS/all").getTotal(), 0.01f);
    }

    @Test
    public void testPriorityIsUsed() {
        rpmServiceManager.getOrCreateRPMService("Test");

        // Create reservoir
        ServiceFactory.getTransactionEventsService().harvestEvents("Test");

        DistributedSamplingPriorityQueue<TransactionEvent> reservoir = new DistributedSamplingPriorityQueue<>(3);

        TransactionEventBuilder eventBuilder = new TransactionEventBuilder();
        eventBuilder.setPriority(.5f);
        TransactionEvent transactionEvent5 = eventBuilder.build();
        reservoir.add(transactionEvent5);
        eventBuilder.setPriority(.6f);
        TransactionEvent transactionEvent6 = eventBuilder.build();
        reservoir.add(transactionEvent6);
        eventBuilder.setPriority(.7f);
        TransactionEvent transactionEvent7 = eventBuilder.build();
        reservoir.add(transactionEvent7);
        eventBuilder.setPriority(.8f);
        TransactionEvent transactionEvent8 = eventBuilder.build();
        reservoir.add(transactionEvent8);
        eventBuilder.setPriority(.9f);
        TransactionEvent transactionEvent9 = eventBuilder.build();
        reservoir.add(transactionEvent9);

        List<TransactionEvent> events = reservoir.asList();
        assertTrue(events.contains(transactionEvent7));
        assertTrue(events.contains(transactionEvent8));
        assertTrue(events.contains(transactionEvent9));
        assertEquals(3, events.size());

        eventBuilder.setPriority(.4f);
        TransactionEvent transactionEvent4 = eventBuilder.build();
        reservoir.add(transactionEvent4);

        eventBuilder.setPriority(.3f);
        TransactionEvent transactionEvent3 = eventBuilder.build();
        reservoir.add(transactionEvent3);

        events = reservoir.asList();
        assertTrue(events.contains(transactionEvent7));
        assertTrue(events.contains(transactionEvent8));
        assertTrue(events.contains(transactionEvent9));
        assertEquals(3, events.size());
    }

    @Test
    public void testEventsByPriority() {

        distributedTraceService.connected(
                rpmServiceManager.getOrCreateRPMService("Test"),
                ServiceFactory.getConfigService().getAgentConfig("Test")
        );

        // Create reservoir
        ServiceFactory.getTransactionEventsService().harvestEvents("Test");

        DistributedSamplingPriorityQueue<TransactionEvent> reservoir =
                ServiceFactory.getTransactionEventsService().getOrCreateDistributedSamplingReservoir("Test");

        float minPriority = 100.0f;
        float maxPriority = 0.0f;

        for (int i = 0; i < 3000; i++) {
            TransactionEvent transactionEvent = Mockito.mock(TransactionEvent.class);
            Transaction tx = Mockito.mock(Transaction.class);
            Mockito.when(tx.getPriorityFromInboundSamplingDecision()).thenReturn(null);
            Float priority = DistributedTraceServiceImplTest.distributedTraceService.calculatePriority(tx, ROOT);
            minPriority = Math.min(priority, minPriority); // Store the smallest priority we've seen
            maxPriority = Math.max(priority, maxPriority); // Store the largest priority we've seen
            when(transactionEvent.getPriority()).thenReturn(priority);
            reservoir.add(transactionEvent);
        }

        for (int i = 0; i < 1000; i++) {
            TransactionEvent transactionEvent = Mockito.mock(TransactionEvent.class);
            Transaction txn = Mockito.mock(Transaction.class);
            when(txn.getPriorityFromInboundSamplingDecision()).thenReturn(1.5f);
            Float priority = DistributedTraceServiceImplTest.distributedTraceService.calculatePriority(txn, REMOTE_PARENT_SAMPLED);
            when(transactionEvent.getPriority()).thenReturn(priority);
            reservoir.add(transactionEvent);
        }

        assertTrue(reservoir.peek().getPriority() > minPriority);
        assertEquals(maxPriority, reservoir.peek().getPriority(), 0.0f);
        assertEquals(4000, reservoir.getNumberOfTries());
        assertTrue(reservoir.getTotalSampledPriorityEvents() >= 1011);

        List<TransactionEvent> events = reservoir.asList();
        int sampled = reservoir.getTotalSampledPriorityEvents();
        //verify that the number of "sampled" events equals the number of events with priority >= 1.0
        for (int i = 0; i < sampled + 1; i++) {
            if (i < sampled) {
                assertTrue(DistributedTraceUtil.isSampledPriority(events.get(i).getPriority()));
            } else {
                assertTrue(events.get(i).getPriority() < 1.0f);
            }
        }

        //harvest in order to examine seen vs sent metrics
        ServiceFactory.getTransactionEventsService().harvestEvents("Test");
        StatsEngine statsEngineForHarvest = ServiceFactory.getStatsService().getStatsEngineForHarvest("Test");
        assertTrue(statsEngineForHarvest.getStats(MetricName.create(
                MetricNames.SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_TRANSACTION_EVENT_SEEN)).hasData());
        assertTrue(statsEngineForHarvest.getStats(MetricName.create(
                MetricNames.SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_TRANSACTION_EVENT_SENT)).hasData());
        assertEquals(reservoir.getNumberOfTries(), statsEngineForHarvest.getStats(MetricName.create(
                MetricNames.SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_TRANSACTION_EVENT_SEEN)).getCallCount());
        assertEquals(2000, statsEngineForHarvest.getStats(MetricName.create(
                MetricNames.SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_TRANSACTION_EVENT_SENT)).getCallCount());
    }

    @Test
    public void testCalculatePriorityRootShouldRouteToAdaptiveSampler(){
        //this method doesn't do much right now. It might in the future.
        IRPMService rpmService = rpmServiceManager.getOrCreateRPMService("Test");
        AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig("Test");
        DistributedTraceServiceImplTest.distributedTraceService.connected(rpmService, agentConfig);
        assertEquals(SamplerFactory.ADAPTIVE, distributedTraceService.getFullGranularitySamplers().get(ROOT).getType());
    }

    @Test
    public void calculatePriorityRemoteParentSampledUsesSampler(){
        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("remote_parent_sampled", "always_on")
                .withSamplerSetting("remote_parent_not_sampled", "always_off")
                .buildMainConfig();

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);

        IRPMService rpmService = rpmServiceManager.getOrCreateRPMService("Test");
        DistributedTraceServiceImplTest.distributedTraceService.connected(rpmService, agentConfig);
        assertEquals(SamplerFactory.ALWAYS_ON, distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_SAMPLED).getType());

        Transaction txn = Mockito.mock(Transaction.class);
        Mockito.when(txn.getPriorityFromInboundSamplingDecision()).thenReturn(1.5f);
        assertEquals(2.0f, distributedTraceService.calculatePriority(txn, REMOTE_PARENT_SAMPLED), 0.0f);
        Mockito.when(txn.getPriorityFromInboundSamplingDecision()).thenReturn(null);
        assertEquals(2.0f, distributedTraceService.calculatePriority(txn, REMOTE_PARENT_SAMPLED), 0.0f);
    }

    @Test
    public void calculatePriorityRemoteParentNotSampledUsesSampler(){
        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("remote_parent_sampled", "always_on")
                .withSamplerSetting("remote_parent_not_sampled", "always_off")
                .buildMainConfig();

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);

        IRPMService rpmService = rpmServiceManager.getOrCreateRPMService("Test");
        DistributedTraceServiceImplTest.distributedTraceService.connected(rpmService, agentConfig);
        assertEquals(SamplerFactory.ALWAYS_ON, distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_SAMPLED).getType());

        Transaction txn = Mockito.mock(Transaction.class);
        Mockito.when(txn.getPriorityFromInboundSamplingDecision()).thenReturn(1.5f);
        assertEquals(0.0f, distributedTraceService.calculatePriority(txn, REMOTE_PARENT_NOT_SAMPLED), 0.0f);
        Mockito.when(txn.getPriorityFromInboundSamplingDecision()).thenReturn(null);
        assertEquals(0.0f, distributedTraceService.calculatePriority(txn, REMOTE_PARENT_NOT_SAMPLED), 0.0f);
    }

    @Test
    public void remoteParentSamplersUseInboundPriorityWhenSetToAdaptive(){
        IRPMService rpmService = rpmServiceManager.getOrCreateRPMService("Test");
        AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig("Test");
        DistributedTraceServiceImplTest.distributedTraceService.connected(rpmService, agentConfig);
        assertEquals(SamplerFactory.ADAPTIVE, distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerFactory.ADAPTIVE, distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_NOT_SAMPLED).getType());

        Transaction txn = Mockito.mock(Transaction.class);
        Mockito.when(txn.getPriorityFromInboundSamplingDecision()).thenReturn(1.5f);
        assertEquals(1.5f, distributedTraceService.calculatePriority(txn, REMOTE_PARENT_SAMPLED), 0.0f);
        assertEquals(1.5f, distributedTraceService.calculatePriority(txn, REMOTE_PARENT_NOT_SAMPLED), 0.0f);
    }

    @Test
    public void testConnectFields() {
        assertNull(DistributedTraceServiceImplTest.distributedTraceService.getApplicationId());
        assertNull(DistributedTraceServiceImplTest.distributedTraceService.getTrustKey());
        assertNull(DistributedTraceServiceImplTest.distributedTraceService.getAccountId());

        IRPMService rpmService = rpmServiceManager.getOrCreateRPMService("Test");

        Map<String, Object> connectInfo = new HashMap<>();
        connectInfo.put(DistributedTracingConfig.ACCOUNT_ID, "accountId");
        connectInfo.put(DistributedTracingConfig.PRIMARY_APPLICATION_ID, "primaryApplicationId");
        connectInfo.put(DistributedTracingConfig.TRUSTED_ACCOUNT_KEY, "trustKey");
        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, Collections.<String, Object>emptyMap(), connectInfo);

        DistributedTraceServiceImplTest.distributedTraceService.connected(rpmService, agentConfig);

        assertEquals("accountId", DistributedTraceServiceImplTest.distributedTraceService.getAccountId());
        assertEquals("primaryApplicationId", DistributedTraceServiceImplTest.distributedTraceService.getApplicationId());
        assertEquals("trustKey", DistributedTraceServiceImplTest.distributedTraceService.getTrustKey());
    }

    @Test
    public void testDTServiceSetsUpDefaultSamplers() {
        assertEquals(SamplerFactory.ADAPTIVE, DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(ROOT).getType());
        assertEquals(SamplerFactory.ADAPTIVE, DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerFactory.ADAPTIVE, DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_NOT_SAMPLED).getType());

        assertEquals(SamplerFactory.ADAPTIVE, DistributedTraceServiceImplTest.distributedTraceService.getPartialGranularitySamplers().get(ROOT).getType());
        assertEquals(SamplerFactory.ADAPTIVE, DistributedTraceServiceImplTest.distributedTraceService.getPartialGranularitySamplers().get(REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerFactory.ADAPTIVE, DistributedTraceServiceImplTest.distributedTraceService.getPartialGranularitySamplers().get(REMOTE_PARENT_NOT_SAMPLED).getType());

        //the samplers should all be the same instance
        Sampler baseSampler = AdaptiveSampler.getSharedInstance();
        assertEquals(baseSampler, DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(ROOT));
        assertEquals(baseSampler,  DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_SAMPLED));
        assertEquals(baseSampler,   DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_NOT_SAMPLED));

        //the sampling target should be the default, 120
        assertEquals(120, ((AdaptiveSampler) DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(ROOT)).getTarget());
    }

    @Test
    public void testDTServiceSetsUpConfiguredSamplers() {
        /*
        distributed_tracing:
          sampler:
            remote_parent_not_sampled: always_off
            full_granularity:
              root:
                trace_id_ratio_based:
                  ratio: 0.6
              remote_parent_sampled: always_on
            partial_granularity:
              enabled: true
              type: reduced
              root:
                adaptive:
                  sampling_target: 15
              remote_parent_sampled: (adaptive)
              remote_parent_not_sampled:
                trace_id_ratio_based:
                  ratio: 0.1
         */

        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("remote_parent_not_sampled", "always_off")
                .withFullGranularitySetting("root", "trace_id_ratio_based", "ratio", 0.6)
                .withFullGranularitySetting("remote_parent_sampled", "always_on")
                .withPartialGranularitySetting("enabled", "true")
                .withPartialGranularitySetting("type", "reduced")
                .withPartialGranularitySetting("root", "adaptive", "sampling_target", 15)
                .withPartialGranularitySetting("remote_parent_not_sampled", "trace_id_ratio_based", "ratio", 0.1)
                .buildMainConfig();

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);

        assertEquals(SamplerFactory.TRACE_RATIO_ID_BASED, distributedTraceService.getFullGranularitySamplers().get(ROOT).getType());
        assertEquals(SamplerFactory.ALWAYS_ON, distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerFactory.ALWAYS_OFF, distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_NOT_SAMPLED).getType());
        assertEquals(SamplerFactory.ADAPTIVE, distributedTraceService.getPartialGranularitySamplers().get(ROOT).getType());
        assertEquals(SamplerFactory.ADAPTIVE, distributedTraceService.getPartialGranularitySamplers().get(REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerFactory.TRACE_RATIO_ID_BASED, distributedTraceService.getPartialGranularitySamplers().get(REMOTE_PARENT_NOT_SAMPLED).getType());
    }

    @Test
    public void testConnectResetsDefaultAdaptiveSamplingTarget() {
        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("remote_parent_sampled", "always_on")
                .withSamplerSetting("remote_parent_not_sampled", "always_off")
                .buildMainConfig();

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);

        assertEquals(SamplerFactory.ADAPTIVE, DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(ROOT).getType());
        assertEquals(120, ((AdaptiveSampler) DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(ROOT)).getTarget());

        IRPMService rpmService = rpmServiceManager.getOrCreateRPMService("Test");
        Map<String, Object> connectInfo = new HashMap<>();
        connectInfo.put("sampling_target", 10);
        agentConfig = AgentHelper.createAgentConfig(true, Collections.<String, Object>emptyMap(), connectInfo);
        DistributedTraceServiceImplTest.distributedTraceService.connected(rpmService, agentConfig);

        assertEquals(SamplerFactory.ADAPTIVE, DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(ROOT).getType());
        assertEquals(10, ((AdaptiveSampler) DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(ROOT)).getTarget());
        assertEquals(SamplerFactory.ALWAYS_ON, DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerFactory.ALWAYS_OFF, DistributedTraceServiceImplTest.distributedTraceService.getFullGranularitySamplers().get(REMOTE_PARENT_NOT_SAMPLED).getType());
    }

    @Test
    public void nothingSampledWhenFullAndPartialDisabled(){
        /*
          distributed_tracing:
            sampler:
              full_granularity:
                enabled: false
                root: always_on
                remote_parent_sampled: always_on
                remote_parent_not_sampled: always_on
              partial_granularity:
                enabled: false (default)
         */

        Map<String, Object> config = new DTConfigMapBuilder()
                .withFullGranularitySetting("enabled", "false")
                .withFullGranularitySetting("root", "always_on")
                .withFullGranularitySetting("remote_parent_sampled", "always_on")
                .withFullGranularitySetting("remote_parent_not_sampled", "always_on")
                .buildMainConfig();

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);

        for (int i = 0; i < 50; i++){
            Transaction tx = Mockito.mock(Transaction.class);
            Mockito.when(tx.getPriorityFromInboundSamplingDecision()).thenReturn(null);

            float priority = distributedTraceService.calculatePriority(tx, ROOT);
            assertFalse(DistributedTraceUtil.isSampledPriority(priority));

            priority = distributedTraceService.calculatePriority(tx, REMOTE_PARENT_SAMPLED);
            assertFalse(DistributedTraceUtil.isSampledPriority(priority));

            priority = distributedTraceService.calculatePriority(tx, REMOTE_PARENT_NOT_SAMPLED);
            assertFalse(DistributedTraceUtil.isSampledPriority(priority));
        }
    }

    @Test
    public void partialSamplersRunWhenFullGranularityDisabled(){
        //This test should sample every transaction as a partial granularity transaction.
        //To verify this, check that the transaction's partial sample type has been set every time the priority is calculated.
        //(Every transaction should be sampled, since all samplers are set to always_on)
        /*
          distributed_tracing:
            sampler:
              full_granularity:
                enabled: false
              partial_granularity:
                enabled: true
                root: always_on
                remote_parent_sampled: always_on
                remote_parent_not_sampled: always_on
                //type: essential (default)
         */

        Map<String, Object> config = new DTConfigMapBuilder()
                .withFullGranularitySetting("enabled", "false")
                .withPartialGranularitySetting("enabled", "true")
                .withPartialGranularitySetting("root", "always_on")
                .withPartialGranularitySetting("remote_parent_sampled",  "always_on")
                .withPartialGranularitySetting("remote_parent_not_sampled",  "always_on")
                .buildMainConfig();

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);

        for (int i = 0; i < 10; i++){
            Transaction tx = Mockito.mock(Transaction.class);
            Mockito.when(tx.getPriorityFromInboundSamplingDecision()).thenReturn(null);
            float priority = distributedTraceService.calculatePriority(tx, ROOT);
            assertTrue(DistributedTraceUtil.isSampledPriority(priority));
            Mockito.verify(tx, Mockito.times(1)).setPartialSampleType(Transaction.PartialSampleType.ESSENTIAL);

            tx = Mockito.mock(Transaction.class);
            Mockito.when(tx.getPriorityFromInboundSamplingDecision()).thenReturn(null);
            priority = distributedTraceService.calculatePriority(tx, REMOTE_PARENT_SAMPLED);
            assertTrue(DistributedTraceUtil.isSampledPriority(priority));
            Mockito.verify(tx, Mockito.times(1)).setPartialSampleType(Transaction.PartialSampleType.ESSENTIAL);

            tx = Mockito.mock(Transaction.class);
            Mockito.when(tx.getPriorityFromInboundSamplingDecision()).thenReturn(null);
            priority = distributedTraceService.calculatePriority(tx, REMOTE_PARENT_NOT_SAMPLED);
            assertTrue(DistributedTraceUtil.isSampledPriority(priority));
            Mockito.verify(tx, Mockito.times(1)).setPartialSampleType(Transaction.PartialSampleType.ESSENTIAL);
        }
    }

    @Test
    public void testSpanEventsDisabled() {
        DistributedTracePayload payloadInterface = configureAndCreatePayload(false, false);
        assertTrue(payloadInterface instanceof DistributedTracePayloadImpl);
        DistributedTracePayloadImpl payload = (DistributedTracePayloadImpl)payloadInterface;
        final String payloadStr = payload.httpSafe();
        final DistributedTracePayloadImpl parsedPayload = new DistributedTracePayloadParser(
            NewRelic.getAgent().getMetricAggregator(), distributedTraceService, Agent.LOG).parse(null, payloadStr);

        assertNotNull(parsedPayload);
        // Payload should not have a guid since span events are disabled
        assertNull(parsedPayload.guid);

        // Check other required fields
        assertEquals("App", parsedPayload.parentType);
        assertEquals("accountId", parsedPayload.accountId);
        assertEquals("primaryApplicationId", parsedPayload.applicationId);
        assertEquals(txn.getSpanProxy().getTraceId(), parsedPayload.traceId);
        assertEquals(txn.getGuid(), parsedPayload.txnId);
    }

    private DistributedTracePayload configureAndCreatePayload(
            boolean excludeNewRelicHeader,
            boolean spanEventsEnabled) {
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, "Test");

        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put(DistributedTracingConfig.ENABLED, true);
        // This will need to change when we implement JAVA-5559
        if (excludeNewRelicHeader) {
            dtConfig.put(DistributedTracingConfig.EXCLUDE_NEWRELIC_HEADER, "true");
        }
        config.put(AgentConfigImpl.DISTRIBUTED_TRACING, dtConfig);

        Map<String, Object> spansConfig = new HashMap<>();
        spansConfig.put(SpanEventsConfig.ENABLED, spanEventsEnabled);
        spansConfig.put(SpanEventsConfig.COLLECT_SPAN_EVENTS, true);
        config.put(AgentConfigImpl.SPAN_EVENTS, spansConfig);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());

        serviceManager.setConfigService(configService);
        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();

        Map<String, Object> connectInfo = new HashMap<>();
        connectInfo.put(DistributedTracingConfig.ACCOUNT_ID, "accountId");
        connectInfo.put(DistributedTracingConfig.PRIMARY_APPLICATION_ID, "primaryApplicationId");
        connectInfo.put(DistributedTracingConfig.TRUSTED_ACCOUNT_KEY, "trustKey");
        connectInfo.put(DistributedTracingConfig.EXCLUDE_NEWRELIC_HEADER, excludeNewRelicHeader);
        AgentConfig connectData = AgentHelper.createAgentConfig(true, Collections.<String, Object>emptyMap(), connectInfo);
        DistributedTraceServiceImplTest.distributedTraceService.connected(serviceManager.getRPMServiceManager().getRPMService(), connectData);

        Transaction.clearTransaction();
        txn = Transaction.getTransaction(true);
        final ClassMethodSignature cms = new ClassMethodSignature("class", "method", "methodDesc");
        final OtherRootTracer tracer = new OtherRootTracer(txn, cms, null, new SimpleMetricNameFormat("metricName"));
        txn.getTransactionActivity().tracerStarted(tracer);

        return distributedTraceService.createDistributedTracePayload(tracer);
    }

    private Transaction txn;

    /**
     * The same logic exists in {@link AnalyticsEvent},
     * {@link com.newrelic.agent.Transaction}, and {@link com.newrelic.agent.tracing.DistributedTraceServiceImpl}.
     */
    @Test
    public void testNumberFormats() {
        Locale.setDefault(new Locale("ar", "AE"));
        nextFloat();

        Locale.setDefault(Locale.GERMANY);
        nextFloat();

        Locale.setDefault(Locale.CHINA);
        nextFloat();

        Locale.setDefault(Locale.US);
        nextFloat();
    }

    private void nextFloat() {
        try {
            DistributedTraceServiceImpl.nextTruncatedFloat();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private TransactionData createTransactionData(Map<String, Object> intrinsicAttributes, long startTimeInMillis, long responseTimeInNanos,
            DistributedTracePayloadImpl payload) {
        long payloadTimestamp = payload != null ? payload.timestamp : 0;
        Transaction tx = Mockito.mock(Transaction.class);
        SpanProxy spanProxy = new SpanProxy();
        spanProxy.acceptDistributedTracePayload(payload);
        CrossProcessTransactionState crossProcessTransactionState = Mockito.mock(CrossProcessTransactionState.class);
        Dispatcher dispatcher = Mockito.mock(Dispatcher.class);
        TransactionTimer timer = Mockito.mock(TransactionTimer.class);

        when(tx.getIntrinsicAttributes()).thenReturn(intrinsicAttributes);
        when(tx.getSpanProxy()).thenReturn(spanProxy);
        when(crossProcessTransactionState.getTripId()).thenReturn("abc123");
        when(tx.getCrossProcessTransactionState()).thenReturn(crossProcessTransactionState);
        when(dispatcher.isWebTransaction()).thenReturn(true);
        when(tx.getDispatcher()).thenReturn(dispatcher);
        when((tx.getTransportDurationInMillis())).thenReturn(startTimeInMillis - payloadTimestamp);
        when(timer.getResponseTimeInNanos()).thenReturn(responseTimeInNanos);
        when(tx.getTransactionTimer()).thenReturn(timer);

        when(tx.isErrorReportableAndNotIgnored()).thenReturn(true);
        when(tx.getTransportType()).thenReturn(TransportType.HTTPS);

        return new TransactionData(tx, 0);
    }

}
