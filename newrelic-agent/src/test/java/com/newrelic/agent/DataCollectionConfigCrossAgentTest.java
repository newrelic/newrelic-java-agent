/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.errors.ThrowableError;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.SpanEventsServiceFactory;
import com.newrelic.agent.service.analytics.CollectorSpanEventReservoirManager;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.service.analytics.EventTestHelper;
import com.newrelic.agent.service.analytics.InsightsServiceImpl;
import com.newrelic.agent.service.analytics.SpanEventFactory;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class DataCollectionConfigCrossAgentTest {

    private static final String APP_NAME = "Test";

    private static MockServiceManager serviceManager;
    private static MockRPMService rpmService;

    private static Map<String, Object> defaultConfig;

    private static Instrumentation savedInstrumentation;
    private static com.newrelic.agent.bridge.Agent savedAgent;
    private static ConfigService configService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        savedInstrumentation = AgentBridge.instrumentation;
        savedAgent = AgentBridge.agent;

        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        defaultConfig = new HashMap<>();
        defaultConfig.put(AgentConfigImpl.APP_NAME, APP_NAME);

        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put("enabled", true);
        defaultConfig.put("distributed_tracing", dtConfig);

        configService = setupConfig(defaultConfig);
        serviceManager.setTransactionTraceService(new TransactionTraceService());
        serviceManager.setTransactionService(new TransactionService());
        serviceManager.setHarvestService(new HarvestServiceImpl());
        serviceManager.setStatsService(new StatsServiceImpl());
        serviceManager.setEnvironmentService(new EnvironmentServiceImpl());
        serviceManager.setAttributesService(new AttributesService());
        AgentBridge.instrumentation = new InstrumentationImpl(Agent.LOG);
        AgentBridge.agent = new AgentImpl(Agent.LOG);

        serviceManager.setCoreService(mock(CoreService.class));
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        ServiceFactory.getServiceManager().start();
        rpmService = (MockRPMService) rpmServiceManager.getRPMService();
    }

    @AfterClass
    public static void afterClass() {
        Transaction.clearTransaction();

        AgentBridge.instrumentation = savedInstrumentation;
        AgentBridge.agent = savedAgent;
    }

    private static ConfigService setupConfig(Map<String, Object> config) {
        Map<String, Object> newConfig = new HashMap<>(defaultConfig);
        newConfig.putAll(config);

        configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(newConfig),
                Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        return configService;
    }

    @Test
    public void testDataCollection() throws Exception {
        File file = AgentHelper.getFile("com/newrelic/agent/cross_agent_tests/data_collection_server_configuration.json");
        JSONArray tests = readJsonAndGetTests(file);
        for (Object currentTest : tests) {
            if (currentTest == null || !(currentTest instanceof JSONObject)) {
                fail("currentTest is null or not an Object, so the input json must be bad.");
            }

            JSONObject test = (JSONObject) currentTest;
            if (!(test.get("test_name") instanceof String)) {
                fail("test failed; test_name unknown (not in the json or not a string)");
            }

            String testName = (String) test.get("test_name");

            try {
                runTest(testName, test);
            } catch (Throwable t) {
                throw new Exception("Test failed: " + testName, t);
            }
        }
    }

    private void runTest(String testName, JSONObject jsonTest) {
        JSONObject connectResponse = (JSONObject) jsonTest.get("connect_response");
        JSONArray expectedDataSeen = (JSONArray) jsonTest.get("expected_data_seen");
        JSONArray expectedEndpointCalls = (JSONArray) jsonTest.get("expected_endpoint_calls");

        // Parse out any data types (and associated counts) that we should expect from this test
        Map<String, Long> expectedDataTypes = new HashMap<>();
        for (Object expectedDataObject : expectedDataSeen) {
            JSONObject expectedData = (JSONObject) expectedDataObject;
            String expectedDataType = (String) expectedData.get("type");
            Long expectedCount = (Long) expectedData.get("count");

            Long expectedDataTypeCount = expectedDataTypes.get(expectedDataType);
            if (expectedDataTypeCount == null) {
                expectedDataTypeCount = 0L;
            }
            expectedDataTypes.put(expectedDataType, expectedCount + expectedDataTypeCount);
        }

        // Parse out endpoint types (and associated counts) that we should expect from this test
        Map<String, Long> expectedEndpointCounts = new HashMap<>();
        for (Object expectedEndpointCall : expectedEndpointCalls) {
            JSONObject expectedData = (JSONObject) expectedEndpointCall;
            String expectedMethod = (String) expectedData.get("method");
            Long expectedCount = (Long) expectedData.get("count");

            Long expectedEndpointCount = expectedEndpointCounts.get(expectedMethod);
            if (expectedEndpointCount == null) {
                expectedEndpointCount = 0L;
            }
            expectedEndpointCounts.put(expectedMethod, expectedCount + expectedEndpointCount);
        }

        // Put each `collect_` property value under its proper subsection (to allow us to inject this config as if it came down from the server)
        Map<String, Object> connectResponseValues = new HashMap<>();
        for (Object connectResponseObject : connectResponse.entrySet()) {
            Map.Entry<String, Object> responseEntry = (Map.Entry<String, Object>) connectResponseObject;

            if (responseEntry.getKey().contains("span")) {
                Map<String, Object> spanEventsConfig = (Map<String, Object>) connectResponseValues.get("span_events");
                if (spanEventsConfig == null) {
                    spanEventsConfig = new HashMap<>();
                    connectResponseValues.put("span_events", spanEventsConfig);
                }
                spanEventsConfig.put(responseEntry.getKey(), responseEntry.getValue());
            } else if (responseEntry.getKey().contains("custom")) {
                Map<String, Object> customInsightsEvents = (Map<String, Object>) connectResponseValues.get("custom_insights_events");
                if (customInsightsEvents == null) {
                    customInsightsEvents = new HashMap<>();
                    connectResponseValues.put("custom_insights_events", customInsightsEvents);
                }
                customInsightsEvents.put(responseEntry.getKey(), responseEntry.getValue());
            } else if (responseEntry.getKey().contains("analytics")) {
                Map<String, Object> transactionEvents = (Map<String, Object>) connectResponseValues.get("transaction_events");
                if (transactionEvents == null) {
                    transactionEvents = new HashMap<>();
                    connectResponseValues.put("transaction_events", transactionEvents);
                }
                transactionEvents.put(responseEntry.getKey(), responseEntry.getValue());
            } else if (responseEntry.getKey().contains("error")) {
                Map<String, Object> errorCollector = (Map<String, Object>) connectResponseValues.get("error_collector");
                if (errorCollector == null) {
                    errorCollector = new HashMap<>();
                    connectResponseValues.put("error_collector", errorCollector);
                }
                errorCollector.put(responseEntry.getKey(), responseEntry.getValue());
            } else if (responseEntry.getKey().contains("traces")) {
                Map<String, Object> transactionTracer = (Map<String, Object>) connectResponseValues.get("transaction_tracer");
                if (transactionTracer == null) {
                    transactionTracer = new HashMap<>();
                    connectResponseValues.put("transaction_tracer", transactionTracer);
                }
                transactionTracer.put(responseEntry.getKey(), responseEntry.getValue());
            }
        }

        // Set the connect response values before creating any events
        setupConfig(connectResponseValues);
        setUpSpanEventsService();

        Transaction.clearTransaction();
        serviceManager.getTransactionService().getTransaction(true);

        // Loop over each data type and create the associated event/trace/whatever
        for (Map.Entry<String, Long> expectedDataType : expectedDataTypes.entrySet()) {
            String type = expectedDataType.getKey();
            switch (type) {
                case "span_event":
                    createAndVerifySpanEvents(expectedDataType.getValue(), expectedEndpointCounts.get("span_event_data"));
                    break;
                case "custom_event":
                    createAndVerifyCustomEvent(expectedDataType.getValue(), expectedEndpointCounts.get("custom_event_data"));
                    break;
                case "transaction_event":
                    createAndVerifyTransactionEvent(expectedDataType.getValue(), expectedEndpointCounts.get("analytic_event_data"));
                    break;
                case "error_event":
                    createAndVerifyErrorEvent(expectedDataType.getValue(), expectedEndpointCounts.get("error_event_data"));
                    break;
                case "error_trace":
                    createAndVerifyErrorTrace(expectedDataType.getValue(), expectedEndpointCounts.get("error_data"));
                    break;
                case "transaction_trace":
                    createAndVerifyTransactionTrace(expectedDataType.getValue(), expectedEndpointCounts.get("transaction_sample_data"));
                    break;
                default:
                    throw new IllegalStateException("Unexpected event type: " + type);
            }
        }
    }

    private void createAndVerifySpanEvents(Long expectedCount, Long expectedEndpointCount) {
        SpanEventsService spanEventsService = setUpSpanEventsService();

        long eventsToCreate = 1;
        if (expectedCount > 1) {
            eventsToCreate = expectedCount;
        }

        for (long i = 0; i < eventsToCreate; i++) {
            spanEventsService.storeEvent(new SpanEventFactory(APP_NAME).setName("Span").build());
        }

        // Verify that the correct number of events were stored in the reservoir
        SamplingPriorityQueue<SpanEvent> eventQueue = spanEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        assertNotNull(eventQueue);
        assertEquals(expectedCount.intValue(), eventQueue.size());

        // Verify that we sent (or didn't send) the appropriate events
        spanEventsService.harvestEvents(APP_NAME);
        int spanEventsSeen = ((MockRPMService) serviceManager.getRPMServiceManager().getRPMService(APP_NAME)).getSpanEventsSeen();
        assertEquals(expectedEndpointCount.intValue(), spanEventsSeen);
    }

    private SpanEventsService setUpSpanEventsService() {
        SpanEventsService spanEventsService = SpanEventsServiceFactory.builder()
                .configService(configService)
                .reservoirManager(new CollectorSpanEventReservoirManager(configService))
                .transactionService(serviceManager.getTransactionService())
                .rpmServiceManager(serviceManager.getRPMServiceManager())
                .build();
        serviceManager.setSpansEventService(spanEventsService);
        return spanEventsService;
    }

    private void createAndVerifyCustomEvent(Long expectedCount, Long expectedEndpointCount) {
        InsightsServiceImpl insightsService = new InsightsServiceImpl();
        serviceManager.setInsights(insightsService);

        long eventsToCreate = 1;
        if (expectedCount > 1) {
            eventsToCreate = expectedCount;
        }

        for (long i = 0; i < eventsToCreate; i++) {
            insightsService.recordCustomEvent("Custom", Collections.<String, Object>emptyMap());
        }

        // Verify that the correct number of events were stored in the reservoir
        DistributedSamplingPriorityQueue<CustomInsightsEvent> eventQueue = insightsService.getReservoir(APP_NAME);
        assertNotNull(eventQueue);
        assertEquals(expectedCount.intValue(), eventQueue.size());

        // Verify that we sent (or didn't send) the appropriate events
        insightsService.harvestEvents(APP_NAME);
        int customEventsSeen = rpmService.getCustomEventsSeen();
        assertEquals(expectedEndpointCount.intValue(), customEventsSeen);
    }

    private void createAndVerifyTransactionEvent(Long expectedCount, Long expectedEndpointCount) {
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = mock(TransactionDataToDistributedTraceIntrinsics.class);
        TransactionEventsService transactionEventsService = new TransactionEventsService(transactionDataToDistributedTraceIntrinsics);
        serviceManager.setTransactionEventsService(transactionEventsService);

        long eventsToCreate = 1;
        if (expectedCount > 1) {
            eventsToCreate = expectedCount;
        }

        for (long i = 0; i < eventsToCreate; i++) {
            TransactionData transactionData = EventTestHelper.generateTransactionDataAndComplete(Collections.<String, Object>emptyMap(), APP_NAME);
            TransactionStats transactionStats = new TransactionStats();
            transactionEventsService.dispatcherTransactionFinished(transactionData, transactionStats);
        }

        // Verify that the correct number of events were stored in the reservoir
        DistributedSamplingPriorityQueue<TransactionEvent> eventQueue = transactionEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        assertNotNull(eventQueue);
        assertEquals(expectedCount.intValue(), eventQueue.size());

        // Verify that we sent (or didn't send) the appropriate events
        transactionEventsService.harvestEvents(APP_NAME);
        int transactionEventsSeen = rpmService.getTransactionEventsSeen();
        assertEquals(expectedEndpointCount.intValue(), transactionEventsSeen);
    }

    private void createAndVerifyErrorEvent(Long expectedCount, Long expectedEndpointCount) {
        ErrorServiceImpl errorService = new ErrorServiceImpl(APP_NAME);
        rpmService.setErrorService(errorService);

        long eventsToCreate = 1;
        if (expectedCount > 1) {
            eventsToCreate = expectedCount;
        }

        for (long i = 0; i < eventsToCreate; i++) {
            ThrowableError error = ThrowableError.builder(errorService.getErrorCollectorConfig(), APP_NAME, "metric", new Throwable(),
                    System.currentTimeMillis()).build();
            errorService.reportError(error);
        }

        // Verify that the correct number of events were stored in the reservoir
        DistributedSamplingPriorityQueue<ErrorEvent> eventQueue = errorService.getReservoir(APP_NAME);
        assertNotNull(eventQueue);
        assertEquals(expectedCount.intValue(), eventQueue.size());

        // Verify that we sent (or didn't send) the appropriate events
        errorService.harvestEvents(APP_NAME);
        int errorEventsSeen = rpmService.getErrorEventsSeen();
        assertEquals(expectedEndpointCount.intValue(), errorEventsSeen);
    }

    private void createAndVerifyErrorTrace(Long expectedCount, Long expectedEndpointCount) {
        ErrorServiceImpl errorService = new ErrorServiceImpl(APP_NAME);
        rpmService.setErrorService(errorService);

        long eventsToCreate = 1;
        if (expectedCount > 1) {
            eventsToCreate = expectedCount;
        }

        for (long i = 0; i < eventsToCreate; i++) {
            ThrowableError error = ThrowableError.builder(errorService.getErrorCollectorConfig(), APP_NAME, "metric", new Throwable(),
                    System.currentTimeMillis()).build();
            errorService.reportError(error);
        }

        // Verify that the correct number of traces were stored in the reservoir
        assertEquals(expectedCount.intValue(), errorService.getTracedErrorsCount());

        // Verify that we sent (or didn't send) the appropriate traces
        errorService.harvestTracedErrors(APP_NAME, new StatsEngineImpl());
        int errorTracesSeen = rpmService.getErrorTracesSeen();
        assertEquals(expectedEndpointCount.intValue(), errorTracesSeen);
    }

    private void createAndVerifyTransactionTrace(Long expectedCount, Long expectedEndpointCount) {
        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        long eventsToCreate = 1;
        if (expectedCount > 1) {
            eventsToCreate = expectedCount;
        }

        for (long i = 0; i < eventsToCreate; i++) {
            TransactionData transactionData = EventTestHelper.generateTransactionDataAndComplete(Collections.<String, Object>emptyMap(), APP_NAME, 10000);
            TransactionStats transactionStats = new TransactionStats();
            transactionTraceService.dispatcherTransactionFinished(transactionData, transactionStats);
        }

        // Verify that we sent (or didn't send) the appropriate traces
        StatsEngine statsEngine = new StatsEngineImpl();
        transactionTraceService.beforeHarvest(APP_NAME, statsEngine);
        transactionTraceService.afterHarvest(APP_NAME);
        int transactionTracesSeen = rpmService.getTransactionTracesSeen();
        assertEquals(expectedEndpointCount.intValue(), transactionTracesSeen);
    }

    private static JSONArray readJsonAndGetTests(File file) throws Exception {
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        JSONArray theTests = null;
        try {
            fr = new FileReader(file);
            theTests = (JSONArray) parser.parse(fr);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                }
            }
        }
        return theTests;
    }
}
