/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.LazyMapImpl;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.ConfigServiceImpl;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.agent.transport.HttpResponseCode;
import com.newrelic.agent.util.TimeConversion;
import org.junit.After;
import org.junit.Test;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.newrelic.agent.service.analytics.EventTestHelper.generateTransactionData;
import static com.newrelic.agent.service.analytics.EventTestHelper.generateTransactionDataAndComplete;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TransactionEventsServiceTest {
    private static final int TEST_RESERVOIR_SIZE = 5;
    private static final String APP_NAME = "TestAppName";
    private static final String APP_NAME_2 = "appName2";
    private ConfigService configService;

    private AgentConfig iAgentConfig;
    private EnvironmentService environmentService;
    MockRPMService rpmService;
    private MockRPMService rpmServiceAppName2;

    TransactionEventsService service;
    private MockRPMServiceManager rpmServiceManager;
    private TransactionDataToDistributedTraceIntrinsics mockDistributedTraceIntrinsics;

    private void setup(boolean enabled, boolean isAttsEnabled, int size) throws Exception {
        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);

        Map<String, Object> settings = new HashMap<>();
        settings.put("app_name", APP_NAME);
        Map<String, Object> events = new HashMap<>();
        settings.put("transaction_events", events);
        events.put("max_samples_stored", size);
        events.put("enabled", enabled);
        Map<String, Object> atts = new HashMap<>();
        settings.put("attributes", atts);
        atts.put("enabled", isAttsEnabled);
        Map<String, Object> distributedTracing = new HashMap<>();
        distributedTracing.put("enabled", true);
        settings.put("distributed_tracing", distributedTracing);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        settings.put("span_events", spanConfig);
        iAgentConfig = AgentConfigImpl.createAgentConfig(settings);

        configService = ConfigServiceFactory.createConfigService(iAgentConfig, settings);
        manager.setConfigService(configService);
        TransactionTraceService transactionTraceService = new TransactionTraceService();
        manager.setTransactionTraceService(transactionTraceService);
        environmentService = new EnvironmentServiceImpl();
        manager.setEnvironmentService(environmentService);
        AttributesService attService = new AttributesService();
        manager.setAttributesService(attService);
        TransactionService transactionService = new TransactionService();
        manager.setTransactionService(transactionService);

        StatsService statsService = new StatsServiceImpl();
        manager.setStatsService(statsService);

        HarvestService harvestService = new MockHarvestService();
        manager.setHarvestService(harvestService);

//        manager.setTransactionEventsService(new TransactionEventsService(mockDistributedTraceIntrinsics));
        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        Map<String, Object> connectInfo = new HashMap<>();
        connectInfo.put(DistributedTracingConfig.ACCOUNT_ID, "1acct234");
        connectInfo.put(DistributedTracingConfig.TRUSTED_ACCOUNT_KEY, "67890");
        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, Collections.<String, Object>emptyMap(), connectInfo);
        distributedTraceService.connected(null, agentConfig);
        manager.setDistributedTraceService(distributedTraceService);

        rpmServiceManager = new MockRPMServiceManager();
        rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmServiceManager.setRPMService(rpmService);
        ErrorServiceImpl errorService = new ErrorServiceImpl(APP_NAME);
        rpmService.setErrorService(errorService);
        rpmServiceAppName2 = (MockRPMService) rpmServiceManager.getOrCreateRPMService(APP_NAME_2);
        rpmServiceAppName2.setErrorService(new ErrorServiceImpl(APP_NAME_2));

        mockDistributedTraceIntrinsics = mock(TransactionDataToDistributedTraceIntrinsics.class);
        service = new TransactionEventsService(mockDistributedTraceIntrinsics);
        manager.setTransactionEventsService(service);
        manager.setRPMServiceManager(rpmServiceManager);
        service.addHarvestableToService(APP_NAME);

        service.doStart();
    }

    // The MockRPMService can be armed to throw exceptions in some cases.
    // We have to be sure and clear that out after every test.

    @After()
    public void afterTest() {
        if (rpmService != null) {
            rpmService.clearSendAnalyticsEventsException();
            rpmServiceAppName2.clearSendAnalyticsEventsException();
        }
    }

    @Test
    public void testDisabled() throws Exception {
        setup(false, true, TEST_RESERVOIR_SIZE);
        assertFalse(service.isEnabled());

        TransactionData transactionData = generateTransactionData(APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        service.harvestEvents(APP_NAME); // populate the eventData map
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        service.dispatcherTransactionFinished(transactionData, transactionStats);
        assertEquals(0, currentEventData.size());
    }

    @Test
    public void distributedTraceIntrinicsAreAdded() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);
        TransactionData transactionData = generateTransactionData(APP_NAME);
        when(mockDistributedTraceIntrinsics.buildDistributedTracingIntrinsics(any(TransactionData.class), eq(true)))
                .thenReturn(Collections.<String, Object>singletonMap("dt-intrinsic", "here I am"));
        TransactionStats transactionStats = new TransactionStats();
        service.dispatcherTransactionFinished(transactionData, transactionStats);
        assertEquals(1, getEventData(APP_NAME).size());
        TransactionEvent result = getEventData(APP_NAME).asList().get(0);
        assertEquals(Collections.<String, Object> singletonMap("dt-intrinsic", "here I am"), result.getDistributedTraceIntrinsics());
    }

    @Test
    public void testSyntheticsBuffering1() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);
        this.rpmService.setSendAnalyticsEventsException(new HttpError("", HttpResponseCode.REQUEST_TIMEOUT, 0));
        TransactionData transactionData = generateSyntheticTransactionData();
        TransactionStats transactionStats = new TransactionStats();
        service.dispatcherTransactionFinished(transactionData, transactionStats);
        service.harvestEvents(APP_NAME);
        assertEquals(1, service.pendingSyntheticsHeaps.size());
        assertEquals(1, service.pendingSyntheticsHeaps.getFirst().size());
    }

    @Test
    public void testSyntheticsBuffering2() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);
        this.rpmService.setSendAnalyticsEventsException(new HttpError("", HttpResponseCode.REQUEST_TIMEOUT, 0));

        // First attempt to enqueue one more than the implementation limit on pending synthetics
        // event buffers, which each buffer being asked to hold one more than the implementation
        // limit number of events.
        for (int i = 0; i < TransactionEventsService.MAX_UNSENT_SYNTHETICS_HOLDERS; ++i) {
            for (int j = 0; j < TransactionEventsService.MAX_SYNTHETIC_EVENTS_PER_APP + 1; ++j) {
                TransactionData transactionData = generateSyntheticTransactionData();
                TransactionStats transactionStats = new TransactionStats();
                service.dispatcherTransactionFinished(transactionData, transactionStats);
            }
            service.harvestEvents(APP_NAME);
        }

        // Check that the buffering is full
        assertEquals(TransactionEventsService.MAX_UNSENT_SYNTHETICS_HOLDERS,
                service.pendingSyntheticsHeaps.size());

        DistributedSamplingPriorityQueue<TransactionEvent> firstPendingSyntheticsHeap = service.pendingSyntheticsHeaps.peek();
        assertNotNull(firstPendingSyntheticsHeap);

        assertEquals(TransactionEventsService.MAX_SYNTHETIC_EVENTS_PER_APP,
                firstPendingSyntheticsHeap.size());

        // Now we want to ensure that overflow keeps the most recent elements and only loses
        // the oldest. We grab the very oldest value from the queue and fail one more buffer's
        // worth of events and then check that the oldest value is gone. The job IDs should
        // all be different because we generate the nanoTime into them.
        String jobId = firstPendingSyntheticsHeap.peek().getSyntheticsJobId();
        for (int j = 0; j < TransactionEventsService.MAX_SYNTHETIC_EVENTS_PER_APP + 1; ++j) {
            TransactionData transactionData = generateSyntheticTransactionData();
            TransactionStats transactionStats = new TransactionStats();
            service.dispatcherTransactionFinished(transactionData, transactionStats);
        }
        service.harvestEvents(APP_NAME);

        firstPendingSyntheticsHeap = service.pendingSyntheticsHeaps.peek();
        assertNotNull(firstPendingSyntheticsHeap);
        assertNotEquals(jobId, firstPendingSyntheticsHeap.peek().getSyntheticsJobId());

        // Finally clear the mock RPM service from throwing exceptions every time we call send
        // Check the catching-up algo by verifying the count goes down the first time, but not
        // all the way to zero. Do this without hard-wiring the exact rate of the catching up.
        int numPendingBuffers = service.pendingSyntheticsHeaps.size();
        assertEquals(TransactionEventsService.MAX_UNSENT_SYNTHETICS_HOLDERS, numPendingBuffers);
        this.rpmService.clearSendAnalyticsEventsException();
        service.harvestEvents(APP_NAME);
        assertNotEquals(0, service.pendingSyntheticsHeaps.size());
        assertTrue(service.pendingSyntheticsHeaps.size() < numPendingBuffers - 1);
    }

    @Test
    public void test() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);
        TransactionData transactionData = generateTransactionData(APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(8,
                TimeUnit.MILLISECONDS);
        service.harvestEvents(APP_NAME); // populate the eventData map
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        service.dispatcherTransactionFinished(transactionData, transactionStats);

        assertEquals(1, currentEventData.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData.peek().getDuration(), 0);
        service.harvestEvents(APP_NAME);
        currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
    }

    @Test
    public void testSendOther() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);
        TransactionActivityInitiator rootTracer = new OtherRootTracer(Transaction.getTransaction(), null, new Object(),
                null);

        TransactionData transactionData = new TransactionDataTestBuilder(APP_NAME, iAgentConfig, new MockDispatcherTracer())
                .setDispatcher(rootTracer.createDispatcher())
                .setFrontendMetricName("Frontend/metricname")
                .build();

        TransactionStats transactionStats = new TransactionStats();
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(8,
                TimeUnit.MILLISECONDS);
        service.harvestEvents(APP_NAME); // populate the eventData map
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        service.dispatcherTransactionFinished(transactionData, transactionStats);
        assertEquals(1, currentEventData.size());
    }

    @Test
    public void testMax() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);
        TransactionData transactionData = generateTransactionData(APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        service.harvestEvents(APP_NAME); // populate the eventData map

        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        for (int i = 0; i < TEST_RESERVOIR_SIZE * 10; i++) {
            service.dispatcherTransactionFinished(transactionData, transactionStats);
        }
        assertEquals(TEST_RESERVOIR_SIZE * 10, currentEventData.getNumberOfTries());

        service.harvestEvents(APP_NAME);
        assertEquals(TEST_RESERVOIR_SIZE * 10, ((MockRPMService) ServiceFactory.getRPMService()).getTransactionEventsSeen());
    }

    @Test
    public void testHarvestableConfigure() throws Exception {
        setup(true, true, 10000);
        service.configureHarvestables(60, 10);
        assertEquals(10, service.getMaxSamplesStored());
    }

    @Test
    public void testTransactionEventFasterHarvest() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, APP_NAME);

        EventTestHelper.setAppName(APP_NAME);
        EventTestHelper.createServiceManager(config);

        service = new TransactionEventsService(mock(TransactionDataToDistributedTraceIntrinsics.class));
        ((MockServiceManager) ServiceFactory.getServiceManager()).setTransactionEventsService(service);

        environmentService = new EnvironmentServiceImpl();
        ((MockServiceManager) ServiceFactory.getServiceManager()).setEnvironmentService(environmentService);

        ServiceManager serviceManager = spy(ServiceFactory.getServiceManager());
        ServiceFactory.setServiceManager(serviceManager);

        HarvestServiceImpl harvestService = spy(new HarvestServiceImpl());
        doReturn(harvestService).when(serviceManager).getHarvestService();
        doReturn(0L).when(harvestService).getInitialDelay();

        service.addHarvestableToService(APP_NAME);

        service.configureHarvestables(60, 3);
        assertEquals(3, service.getMaxSamplesStored());

        service.doStart();

        Map<String, Object> connectionInfo = new HashMap<>();
        Map<String, Object> eventHarvest = new HashMap<>();
        Map<String, Object> harvestLimits = new HashMap<>();
        eventHarvest.put("report_period_ms", 5000L); // 5 is the lowest allowable value
        eventHarvest.put("harvest_limits", harvestLimits);
        harvestLimits.put("analytic_event_data", 100L);
        connectionInfo.put("event_harvest_config", eventHarvest);

        harvestService.startHarvestables(ServiceFactory.getRPMService(), AgentConfigImpl.createAgentConfig(connectionInfo));
        getEventData(APP_NAME);

        Thread.sleep(500);

        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = createAndSendTransaction();
        assertEquals(1, currentEventData.size());
        Thread.sleep(6000);
        checkForEvent();

        assertEquals(1, currentEventData.size());
        createAndSendTransaction();
        Thread.sleep(6000);
        checkForEvent();
    }

    private DistributedSamplingPriorityQueue<TransactionEvent> createAndSendTransaction() {
        TransactionData transactionData = generateTransactionData(APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(8,
                TimeUnit.MILLISECONDS);
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        service.dispatcherTransactionFinished(transactionData, transactionStats);
        return currentEventData;
    }

    private void checkForEvent() {
        assertEquals(1, ((MockRPMService) ServiceFactory.getRPMService()).getEvents().size());

        StatsEngine statsEngineForHarvest = ServiceFactory.getStatsService().getStatsEngineForHarvest(
                EventTestHelper.APP_NAME);
        assertTrue(statsEngineForHarvest.getStats(MetricName.create(
                MetricNames.SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_TRANSACTION_EVENT_SEEN)).hasData());
        assertTrue(statsEngineForHarvest.getStats(MetricName.create(
                MetricNames.SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_TRANSACTION_EVENT_SENT)).hasData());

        ((MockRPMService) ServiceFactory.getRPMService()).clearEvents();
    }

    @Test
    public void testErrorSavesData() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);
        TransactionData transactionData = generateTransactionData(APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        service.harvestEvents(APP_NAME); // populate the eventData map

        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        for (int i = 0; i < TEST_RESERVOIR_SIZE * 2; i++) {
            service.dispatcherTransactionFinished(transactionData, transactionStats);
        }
        assertEquals(TEST_RESERVOIR_SIZE * 2, currentEventData.getNumberOfTries());

        service.harvestEvents(APP_NAME);
        assertEquals(TEST_RESERVOIR_SIZE * 2, currentEventData.getNumberOfTries());
    }

    @Test
    public void testJSONStreaming() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);
        TransactionData transactionData = generateTransactionData(APP_NAME);
        TransactionStats transactionStatsEmpty = new TransactionStats();
        TransactionStats transactionStats = new TransactionStats();
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(1,
                TimeUnit.MILLISECONDS);
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.OTHER_TRANSACTION).recordResponseTime(2,
                TimeUnit.MILLISECONDS);
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.QUEUE_TIME).recordResponseTime(3,
                TimeUnit.MILLISECONDS);
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.EXTERNAL_ALL).recordResponseTime(4,
                TimeUnit.MILLISECONDS);
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(DatastoreMetrics.ALL).recordResponseTime(5,
                TimeUnit.MILLISECONDS);
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.GC_CUMULATIVE).recordResponseTime(6,
                TimeUnit.MILLISECONDS);
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.EXTERNAL_ALL).recordResponseTime(7,
                TimeUnit.MILLISECONDS);
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(DatastoreMetrics.ALL).recordResponseTime(8,
                TimeUnit.MILLISECONDS);
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(DatastoreMetrics.ALL).recordResponseTime(9,
                TimeUnit.MILLISECONDS);

        service.dispatcherTransactionFinished(transactionData, transactionStatsEmpty);
        service.dispatcherTransactionFinished(transactionData, transactionStats);
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        TransactionEvent event = currentEventData.poll();
        Writer writer = new StringWriter();
        event.writeJSONString(writer);
        String json = writer.toString();
        assertNotNull(json);
        TransactionEvent eventFull = currentEventData.poll();
        Writer writerFull = new StringWriter();
        eventFull.writeJSONString(writerFull);
        String jsonFull = writerFull.toString();
        assertNotNull(jsonFull);
        assertNotEquals(json, jsonFull);
        assertTrue(jsonFull.contains("\"externalCallCount\":2.0"));
        assertTrue(jsonFull.contains("\"externalDuration\":0.011"));
        assertTrue(jsonFull.contains("\"databaseCallCount\":3.0"));
        assertTrue(jsonFull.contains("\"databaseDuration\":0.022"));
        assertTrue(json.length() < jsonFull.length());
    }

    @Test
    public void testUserParameters() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);
        Map<String, Object> userParams = new LazyMapImpl<>();
        userParams.put("key1", "value1");
        userParams.put("key2", "value2");
        TransactionData transactionData = generateTransactionDataAndComplete(userParams, APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(8,
                TimeUnit.MILLISECONDS);

        service.harvestEvents(APP_NAME); // populate the eventData map
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        service.dispatcherTransactionFinished(transactionData, transactionStats);
        assertEquals(1, currentEventData.size());

        final TransactionEvent queueHead = currentEventData.peek();
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, queueHead.getDuration(), 0);
        final Map<String, Object> attributes = queueHead.getUserAttributesCopy();
        assertEquals(2, attributes.size());
        assertEquals("value1", attributes.get("key1"));
        assertEquals("value2", attributes.get("key2"));
        service.harvestEvents(APP_NAME);
        currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
    }

    @Test
    public void testUserParametersDisabled() throws Exception {
        setup(true, false, TEST_RESERVOIR_SIZE);
        Map<String, Object> userParams = new LazyMapImpl<>();
        userParams.put("key1", "value1");
        userParams.put("key2", "value2");
        TransactionData transactionData = generateTransactionDataAndComplete(userParams, APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(8,
                TimeUnit.MILLISECONDS);
        service.harvestEvents(APP_NAME); // populate the eventData map
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        service.dispatcherTransactionFinished(transactionData, transactionStats);

        assertEquals(1, currentEventData.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData.peek().getDuration(), 0);
        assertTrue(currentEventData.peek().getUserAttributesCopy().isEmpty());
        service.harvestEvents(APP_NAME);
        currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
    }

    @Test
    public void testDifferentAppNamesAllEnabled() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);

        // default app name
        TransactionData transactionData = generateTransactionData(APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(8,
                TimeUnit.MILLISECONDS);
        // second app name
        String appName2 = "secondAppName";
        rpmServiceManager.getOrCreateRPMService(appName2);
        TransactionData transactionData2 = generateTransactionDataAndComplete(Collections.<String, Object>emptyMap(),
                appName2);
        TransactionStats transactionStats2 = new TransactionStats();
        transactionStats2.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(9,
                TimeUnit.MILLISECONDS);
        // third app name
        String appName3 = "thirdAppName";
        rpmServiceManager.getOrCreateRPMService(appName3);
        TransactionData transactionData3 = generateTransactionDataAndComplete(Collections.<String, Object>emptyMap(),
                appName3);
        TransactionStats transactionStats3 = new TransactionStats();
        transactionStats3.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        service.harvestEvents(APP_NAME); // populate the eventData map
        service.harvestEvents(appName2); // populate the eventData map
        service.harvestEvents(appName3); // populate the eventData map
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData2 = getEventData(appName2);
        assertEquals(0, currentEventData2.size());
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData3 = getEventData(appName3);
        assertEquals(0, currentEventData3.size());
        service.dispatcherTransactionFinished(transactionData, transactionStats);
        service.dispatcherTransactionFinished(transactionData2, transactionStats2);
        service.dispatcherTransactionFinished(transactionData3, transactionStats3);

        assertEquals(1, currentEventData.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData.peek().getDuration(), 0);
        assertEquals(1, currentEventData2.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData2.peek().getDuration(), 0);
        assertEquals(1, currentEventData3.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData3.peek().getDuration(), 0);

        service.harvestEvents(APP_NAME);
        currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());

        service.harvestEvents(appName2);
        currentEventData2 = getEventData(appName2);
        assertEquals(0, currentEventData2.size());

        service.harvestEvents(appName3);
        currentEventData3 = getEventData(appName3);
        assertEquals(0, currentEventData3.size());
    }

    @Test
    public void testDifferentAppNamesOneDisabled() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);

        // server side says app name 2 disabled
        Map<String, Object> data = new HashMap<>();
        data.put("collect_analytics_events", Boolean.FALSE);
        ((ConfigServiceImpl) configService).connected(rpmServiceAppName2, data);

        // default app name
        TransactionData transactionData = generateTransactionData(APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(8,
                TimeUnit.MILLISECONDS);
        // second app name
        TransactionData transactionData2 = generateTransactionDataAndComplete(Collections.<String, Object>emptyMap(),
                APP_NAME_2);
        TransactionStats transactionStats2 = new TransactionStats();
        transactionStats2.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(9,
                TimeUnit.MILLISECONDS);
        // third app name
        String appName3 = "thirdAppName";
        rpmServiceManager.getOrCreateRPMService(appName3);
        TransactionData transactionData3 = generateTransactionDataAndComplete(Collections.<String, Object>emptyMap(),
                appName3);
        TransactionStats transactionStats3 = new TransactionStats();
        transactionStats3.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        service.harvestEvents(APP_NAME); // populate the eventData map
        service.harvestEvents(APP_NAME_2); // populate the eventData map
        service.harvestEvents(appName3); // populate the eventData map
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData2 = getEventData(APP_NAME_2);
        assertEquals(0, currentEventData2.size());
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData3 = getEventData(appName3);
        assertEquals(0, currentEventData3.size());

        service.dispatcherTransactionFinished(transactionData, transactionStats);
        service.dispatcherTransactionFinished(transactionData2, transactionStats2);
        service.dispatcherTransactionFinished(transactionData3, transactionStats3);

        assertEquals(1, currentEventData.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData.peek().getDuration(), 0);
        currentEventData2 = getEventData(APP_NAME_2);
        assertNull(currentEventData2);
        assertEquals(1, currentEventData3.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData3.peek().getDuration(), 0);

        service.harvestEvents(APP_NAME);
        currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());

        service.harvestEvents(appName3);
        currentEventData3 = getEventData(appName3);
        assertEquals(0, currentEventData3.size());
    }

    @Test
    public void testDifferentAppNamesOneDisabledMiddle() throws Exception {
        setup(true, true, TEST_RESERVOIR_SIZE);

        // default app name
        TransactionData transactionData = generateTransactionData(APP_NAME);
        TransactionStats transactionStats = new TransactionStats();
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(8,
                TimeUnit.MILLISECONDS);
        // second app name
        TransactionData transactionData2 = generateTransactionDataAndComplete(Collections.<String, Object>emptyMap(),
                APP_NAME_2);
        TransactionStats transactionStats2 = new TransactionStats();
        transactionStats2.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(9,
                TimeUnit.MILLISECONDS);
        // third app name
        String appName3 = "thirdAppName";
        TransactionData transactionData3 = generateTransactionDataAndComplete(Collections.<String, Object>emptyMap(),
                appName3);
        TransactionStats transactionStats3 = new TransactionStats();
        transactionStats3.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        service.harvestEvents(APP_NAME); // populate the eventData map
        service.harvestEvents(APP_NAME_2); // populate the eventData map
        service.harvestEvents(appName3); // populate the eventData map
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData = getEventData(APP_NAME);
        assertEquals(0, currentEventData.size());
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData2 = getEventData(APP_NAME_2);
        assertEquals(0, currentEventData2.size());
        DistributedSamplingPriorityQueue<TransactionEvent> currentEventData3 = getEventData(appName3);
        assertEquals(0, currentEventData3.size());

        service.dispatcherTransactionFinished(transactionData, transactionStats);
        service.dispatcherTransactionFinished(transactionData2, transactionStats2);
        service.dispatcherTransactionFinished(transactionData3, transactionStats3);

        assertEquals(1, currentEventData.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData.peek().getDuration(), 0);
        assertEquals(1, currentEventData2.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData2.peek().getDuration(), 0);
        assertEquals(1, currentEventData3.size());
        assertEquals(100f / TimeConversion.MILLISECONDS_PER_SECOND, currentEventData3.peek().getDuration(), 0);

        // server side says app name 2 disabled
        Map<String, Object> data = new HashMap<>();
        data.put("collect_analytics_events", Boolean.FALSE);
        ((ConfigServiceImpl) configService).connected(rpmServiceAppName2, data);

        // second time with second name
        transactionData2 = generateTransactionDataAndComplete(Collections.<String, Object>emptyMap(), APP_NAME_2);
        transactionStats2 = new TransactionStats();
        transactionStats2.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTime(9,
                TimeUnit.MILLISECONDS);
        service.dispatcherTransactionFinished(transactionData2, transactionStats2);
        currentEventData2 = getEventData(APP_NAME_2);
        // since the second app has been disabled - this should be false
        assertNull(currentEventData2);
    }

    private TransactionData generateSyntheticTransactionData() {
        long durationInMillis = 1000 + System.nanoTime() % 1000;
        long nt = System.nanoTime();

        MockDispatcher dispatcher = new MockDispatcher();
        MockDispatcherTracer rootTracer = new MockDispatcherTracer();
        rootTracer.setDurationInMilliseconds(durationInMillis);
        rootTracer.setStartTime(System.nanoTime());
        rootTracer.setEndTime(System.nanoTime() + TimeUnit.NANOSECONDS.convert(durationInMillis, TimeUnit.MILLISECONDS));

        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

        return new TransactionDataTestBuilder(APP_NAME, agentConfig, rootTracer)
                .setDispatcher(dispatcher)
                .setRequestUri("/mock/synthetic/transaction")
                .setFrontendMetricName("/mock/synthetic/transaction")
                .setSynJobId("Job" + nt)
                .setSynMonitorId("Monitor" + nt)
                .setSynResourceId("Resource" + nt)
                .build();
    }

    private DistributedSamplingPriorityQueue<TransactionEvent> getEventData(String appName) {
        return service.getDistributedSamplingReservoir(appName);
    }
}
