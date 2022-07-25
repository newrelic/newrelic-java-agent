/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.newrelic.agent.*;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.attributes.CrossAgentInput;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.api.agent.TransportType;
import com.newrelic.agent.config.*;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.SpanEventsServiceFactory;
import com.newrelic.agent.service.analytics.SpanEventCreationDecider;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.stats.*;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class W3CTraceContextCrossAgentTest {
    private MockServiceManager serviceManager;
    private DistributedTraceServiceImpl distributedTraceService;
    private StatsServiceImpl statsService;

    private Instrumentation savedInstrumentation;
    private com.newrelic.agent.bridge.Agent savedAgent;

    private SpanEventsService spanEventService;

    private final String APP_NAME = "Test";

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public JSONObject testData;
    private ReservoirManager<SpanEvent> reservoirManager;
    private SamplingPriorityQueue<SpanEvent> eventPool;
    private TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws Exception {
        JSONArray tests = CrossAgentInput.readJsonAndGetTests("com/newrelic/agent/cross_agent_tests/distributed_tracing/trace_context.json");
        List<Object[]> result = new LinkedList<>();
        for (Object test : tests) {
            JSONObject testObject = (JSONObject) test;
            String name = (String) testObject.get("test_name");
            result.add(new Object[]{name, testObject});
        }
        return result;
    }

    @Before
    public void setup() throws Exception {
        savedInstrumentation = AgentBridge.instrumentation;
        savedAgent = AgentBridge.agent;

        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        Map<String, Object> config = Maps.newHashMap();
        config.put(AgentConfigImpl.APP_NAME, APP_NAME);

        Map<String, Object> dtConfig = Maps.newHashMap();
        dtConfig.put("enabled", true);
        dtConfig.put("exclude_newrelic_header", true);
        config.put("distributed_tracing", dtConfig);
        Map<String, Object> spanConfig = Maps.newHashMap();
        spanConfig.put("collect_span_events", true);
        config.put("span_events", spanConfig);


        ConfigService configService = setupConfig(config);
        serviceManager.setTransactionTraceService(new TransactionTraceService());
        serviceManager.setTransactionService(new TransactionService());

        distributedTraceService = new DistributedTraceServiceImpl();
        transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));

        serviceManager.setHarvestService(new HarvestServiceImpl());
        statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);
        serviceManager.setEnvironmentService(new EnvironmentServiceImpl());
        serviceManager.setAttributesService(new AttributesService());
        AgentBridge.instrumentation = new InstrumentationImpl(Agent.LOG);
        AgentBridge.agent = new AgentImpl(Agent.LOG);

        CoreService coreService = Mockito.mock(CoreService.class);
        when(coreService.isEnabled()).thenReturn(true);
        serviceManager.setCoreService(coreService);
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        ServiceFactory.getServiceManager().start();

        ServiceFactory.getTransactionService().addTransactionListener(distributedTraceService);

        spanEventService = createSpanEventService(configService, transactionDataToDistributedTraceIntrinsics);
        serviceManager.setDistributedTraceService(distributedTraceService);
        serviceManager.setSpansEventService(spanEventService);
    }

    @After
    public void tearDown() {
        AgentBridge.agent = savedAgent;
        AgentBridge.instrumentation = savedInstrumentation;
    }

    private ConfigService setupConfig(Map<String, Object> config) {
        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config),
                Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        reservoirManager = new MockSpanEventReservoirManager(configService);
        spanEventService = createSpanEventService(configService, transactionDataToDistributedTraceIntrinsics);
        serviceManager.setSpansEventService(spanEventService);
        return configService;
    }

    private SpanEventsService createSpanEventService(ConfigService configService,
            TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics) {
        return SpanEventsServiceFactory.builder()
                .configService(configService)
                .reservoirManager(reservoirManager)
                .transactionService(serviceManager.getTransactionService())
                .rpmServiceManager(serviceManager.getRPMServiceManager())
                .spanEventCreationDecider(new SpanEventCreationDecider(configService))
                .environmentService(ServiceFactory.getEnvironmentService())
                .transactionDataToDistributedTraceIntrinsics(transactionDataToDistributedTraceIntrinsics)
                .build();
    }

    @Test
    public void testTraceContext() throws Exception {
        String testName = (String) testData.get("test_name");
        String accountKey = (String) testData.get("trusted_account_key");
        String accountId = (String) testData.get("account_id");
        String transportType = (String) testData.get("transport_type");
        Boolean webTransaction = (Boolean) testData.get("web_transaction");
        Boolean raisesException = (Boolean) testData.get("raises_exception");
        Boolean forceSampledTrue = (Boolean) testData.get("force_sampled_true");
        Boolean spanEventsEnabled = (Boolean) testData.get("span_events_enabled");
        replaceConfig(spanEventsEnabled);

        System.out.println("Running test: " + testName);

        JSONArray outbound_payloads = (JSONArray) testData.get("outbound_payloads");
        JSONArray inbound_payloads = (JSONArray) testData.get("inbound_headers");
        List expectedMetrics = (List) testData.get("expected_metrics");

        Map<String, Object> intrinsics = (Map<String, Object>) testData.get("intrinsics");
        Map<String, Object> commonAssertions = intrinsics == null ? Collections.<String, Object>emptyMap() : (Map<String, Object>) intrinsics.get("common");
        List targetEvents = intrinsics == null ? Collections.emptyList() : (ArrayList) intrinsics.get("target_events");
        Map<String, Object> transactionAssertions =
                intrinsics == null ? Collections.<String, Object>emptyMap() : (Map<String, Object>) intrinsics.get("Transaction");
        Map<String, Object> spanAssertions = intrinsics == null ? Collections.<String, Object>emptyMap() : (Map<String, Object>) intrinsics.get("Span");

        Map<String, Object> connectInfo = Maps.newHashMap();
        connectInfo.put(DistributedTracingConfig.ACCOUNT_ID, accountId);
        connectInfo.put(DistributedTracingConfig.TRUSTED_ACCOUNT_KEY, accountKey);
        connectInfo.put(DistributedTracingConfig.PRIMARY_APPLICATION_ID, "2827902");
        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, Collections.<String, Object>emptyMap(), connectInfo);
        distributedTraceService.connected(null, agentConfig);

        Transaction.clearTransaction();
        TransactionActivity.clear();
        spanEventService.clearReservoir();

        Transaction tx = Transaction.getTransaction();
        TransactionData transactionData = new TransactionData(tx, 0);
        TransactionStats transactionStats = transactionData.getTransaction().getTransactionActivity().getTransactionStats();

        eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);

        List<String> parents = Lists.newArrayList();
        List<String> states = Lists.newArrayList();
        List<String> newrelic = Lists.newArrayList();
        if (inbound_payloads != null) {
            for (Object payload : inbound_payloads) {
                JSONObject j = (JSONObject) payload;
                if (j.get("traceparent") != null) {
                    parents.addAll(Lists.newArrayList(String.valueOf(j.get("traceparent"))));
                }
                if (j.get("tracestate") != null) {
                    states.addAll(Lists.newArrayList(String.valueOf(j.get("tracestate"))));
                }
                if (j.get("newrelic") != null) {
                    newrelic.addAll(Lists.newArrayList(String.valueOf(j.get("newrelic"))));
                }
            }
        }

        MockHttpRequest httpRequest = new MockHttpRequest();
        for (String parent : parents) {
            httpRequest.setHeader("traceparent", parent);
        }
        for (String state : states) {
            httpRequest.setHeader("tracestate", state);
        }
        for (String header : newrelic) {
            httpRequest.setHeader("newrelic", header);
        }

        Tracer rootTracer;
        if (webTransaction) {
            rootTracer = TransactionAsyncUtility.createAndStartDispatcherTracer(this, "WebTransaction", httpRequest);
        } else {
            rootTracer = TransactionAsyncUtility.createOtherTracer("OtherTransaction");
            tx.getTransactionActivity().tracerStarted(rootTracer);
            tx.provideRawHeaders(httpRequest);
        }

        if (raisesException) {
            tx.setThrowable(new Throwable(), TransactionErrorPriority.API, false);
        }

        setTransportType(tx, transportType);

        if (forceSampledTrue && tx.getPriority() < 1) {
            tx.setPriorityIfNotNull(new Random().nextFloat() + 1.0f);
        }
        if (outbound_payloads != null) {

            for (Object assertion : outbound_payloads) {
                MockHttpResponse mockHttpResponse = new MockHttpResponse();
                tx.getCrossProcessState().processOutboundRequestHeaders(mockHttpResponse);
                JSONObject payloadAssertions = (JSONObject) assertion;
                String traceparent = mockHttpResponse.getHeader("traceparent");
                String tracestate = mockHttpResponse.getHeader("tracestate");
                if (traceparent != null) {
                    W3CTraceParent traceParent = W3CTraceParentParser.parseHeaders(Collections.singletonList(traceparent));
                    assertOutboundTraceParentPayload(payloadAssertions, traceParent);
                } else {
                    assertOutboundTraceParentPayload(payloadAssertions, null);
                }
                if (tracestate != null) {
                    assertOutboundTraceStatePayload(payloadAssertions,
                            W3CTraceStateSupport.parseHeaders(Collections.singletonList(mockHttpResponse.getHeader("tracestate"))));
                } else {
                    assertOutboundTraceStatePayload(payloadAssertions, null);
                }
            }
        }

        rootTracer.finish(Opcodes.RETURN, 0);
        List<SpanEvent> spans = eventPool.asList();
        TransactionEvent transactionEvent = serviceManager.getTransactionEventsService().createEvent(transactionData, transactionStats, "wat");
        JSONObject txnEvents = serializeAndParseEvents(transactionEvent);

        StatsEngine statsEngine = statsService.getStatsEngineForHarvest(APP_NAME);
        assertExpectedMetrics(expectedMetrics, statsEngine);

        for (Object event : targetEvents) {
            if (event.toString().startsWith("Transaction") && transactionAssertions != null) {
                assertTransactionEvents(transactionAssertions, txnEvents);
                assertTransactionEvents(commonAssertions, txnEvents);
            } else if (event.toString().startsWith("Span") && spanAssertions != null) {
                assertSpanEvents(spanAssertions, spans);
                assertSpanEvents(commonAssertions, spans);
            }
        }
    }

    private void replaceConfig(boolean spanEventsEnabled) {
        Map<String, Object> config = Maps.newHashMap();
        config.put(AgentConfigImpl.APP_NAME, APP_NAME);

        Map<String, Object> dtConfig = Maps.newHashMap();
        dtConfig.put("enabled", true);
        dtConfig.put("exclude_newrelic_header", true);
        config.put("distributed_tracing", dtConfig);

        Map<String, Object> spansConfig = Maps.newHashMap();
        spansConfig.put("enabled", spanEventsEnabled);
        spansConfig.put("collect_span_events", true);
        config.put("span_events", spansConfig);

        setupConfig(config);
    }

    private void assertOutboundTraceParentPayload(JSONObject payloadAssertions, W3CTraceParent payload) {
        if (payloadAssertions == null) {
            return;
        }

        JSONArray expected = (JSONArray) payloadAssertions.get("expected");
        JSONObject exact = (JSONObject) payloadAssertions.get("exact");

        if (exact != null) {
            assertEquals(exact.get("traceparent.version"), payload.getVersion());
            if (exact.containsKey("traceparent.trace_id")) {
                assertEquals(exact.get("traceparent.trace_id"), payload.getTraceId());
            }
            if (exact.containsKey("traceparent.trace_flags")) {
                assertEquals(exact.get("traceparent.trace_flags"), String.format("%02X", payload.getFlags()));
            }
        }

        if (expected != null) {
            if (expected.contains("traceparent.version")) {
                assertNotNull(payload.getVersion());
            }
            if (expected.contains("traceparent.trace_id")) {
                assertNotNull(payload.getTraceId());
            }
            if (expected.contains("traceparent.parent_id")) {
                assertNotNull(payload.getParentId());
            }
        }
    }

    private void assertOutboundTraceStatePayload(JSONObject payloadAssertions, W3CTraceState payload) {
        if (payloadAssertions == null) {
            return;
        }

        JSONArray expected = (JSONArray) payloadAssertions.get("expected");
        JSONArray unexpected = (JSONArray) payloadAssertions.get("unexpected");
        JSONObject exact = (JSONObject) payloadAssertions.get("exact");
        JSONArray vendors = (JSONArray) payloadAssertions.get("vendors");

        if (exact != null) {
            if (exact.containsKey("tracestate.tenant_id")) {
                assertEquals(exact.get("tracestate.tenant_id"), payload.getTrustKey());
            }
            if (exact.containsKey("tracestate.version")) {
                assertEquals(((Long) exact.get("tracestate.version")).intValue(), payload.getVersion());
            }
            if (exact.containsKey("tracestate.parent_type")) {
                assertEquals(((Long) exact.get("tracestate.parent_type")).intValue(), payload.getParentType().value);
            }
            if (exact.containsKey("tracestate.parent_account_id")) {
                assertEquals(exact.get("tracestate.parent_account_id"), payload.getAccountId());
            }
            if (exact.containsKey("tracestate.parent_application_id")) {
                assertEquals(exact.get("tracestate.parent_application_id"), payload.getApplicationId());
            }
            if (exact.containsKey("tracestate.sampled")) {
                assertEquals(exact.get("tracestate.sampled"), payload.getSampled().booleanValue());
            }
            if (exact.containsKey("tracestate.priority")) {
                assertEquals((double) exact.get("tracestate.priority"), (double) payload.getPriority(), 0.00001);
            }
        }

        if (expected != null) {
            if (expected.contains("tracestate.tenant_id")) {
                assertNotNull(payload.getTrustKey());
            }
            if (expected.contains("tracestate.parent_type")) {
                assertNotNull(payload.getParentType());
            }
            if (expected.contains("tracestate.parent_account_id")) {
                assertNotNull(payload.getAccountId());
            }
            if (expected.contains("tracestate.parent_application_id")) {
                assertNotNull(payload.getApplicationId());
            }
            if (expected.contains("tracestate.sampled")) {
                assertNotEquals(Sampled.UNKNOWN, payload.getSampled());
            }
            if (expected.contains("tracestate.priority")) {
                assertNotNull(payload.getPriority());
            }
        }

        if (unexpected != null && payload != null) {
            if (unexpected.contains("tracestate.tenant_id")) {
                assertNull(payload.getTrustKey());
            }
            if (unexpected.contains("tracestate.parent_type")) {
                assertNull(payload.getParentType());
            }
            if (unexpected.contains("tracestate.parent_account_id")) {
                assertNull(payload.getAccountId());
            }
            if (unexpected.contains("tracestate.parent_application_id")) {
                assertNull(payload.getApplicationId());
            }
            if (unexpected.contains("tracestate.sampled")) {
                assertNotEquals(Sampled.UNKNOWN, payload.getSampled());
            }
            if (unexpected.contains("tracestate.priority")) {
                assertNull(payload.getPriority());
            }

            if (vendors != null) {
                assertEquals(payload.getVendorStates().size(), vendors.size());
                for (int i = 0; i < payload.getVendorStates().size(); i++) {
                    String expectedVendorState = payload.getVendorStates().get(i).split("=")[0];
                    String actualVendorState = (String) vendors.get(i);
                    assertEquals(expectedVendorState, actualVendorState);
                }
            }
        }
    }

    private void setTransportType(Transaction tx, String transportType) {
        tx.setTransportType(TransportType.Unknown);

        try {
            tx.setTransportType(TransportType.valueOf(transportType));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void assertSpanEvents(Map<String, Object> spanEventAssertions, List<SpanEvent> spanEventsList) {
        if (spanEventAssertions == null) {
            return;
        }

        for (Map.Entry<String, Object> event : spanEventAssertions.entrySet()) {
            Map<String, Object> intrinsics = spanEventsList.get(0).getIntrinsics();
            if (event.getKey().startsWith("expected")) {
                for (Object val : (JSONArray) event.getValue()) {
                    assertTrue(intrinsics.containsKey(val.toString()));
                }
            } else if (event.getKey().startsWith("exact")) {
                Map<String, Object> exact = (Map<String, Object>) event.getValue();
                for (Map.Entry<String, Object> entry : exact.entrySet()) {
                    final String message = "Expected: " + entry.getKey();
                    final String message1 = "Expected: " + entry.getValue() + " for: " + entry.getKey();

                    if (entry.getKey().equals("priority")) {
                        // Priority
                        assertTrue(message, intrinsics.containsKey(entry.getKey()));
                        Double expectedValue = (Double) entry.getValue();
                        assertEquals(message1, expectedValue.floatValue(), (Float) intrinsics.get(entry.getKey()), 0.0000001f);
                    } else {
                        assertTrue(message, intrinsics.containsKey(entry.getKey()));
                        assertEquals(message1, entry.getValue(), intrinsics.get(entry.getKey()));
                    }
                }
            } else if (event.getKey().startsWith("unexpected")) {
                for (Object val : (JSONArray) event.getValue()) {
                    final String message = "Did not expect: " + val.toString();
                    assertFalse(message, intrinsics.containsKey(val.toString()));
                }
            }
        }
    }

    private void assertTransactionEvents(Map<String, Object> transactionAssertions, JSONObject transactionEvent) {
        for (Map.Entry<String, Object> event : transactionAssertions.entrySet()) {
            if (event.getKey().startsWith("expected")) {
                for (Object val : (JSONArray) event.getValue()) {
                    assertTrue("Expected: " + val.toString(), transactionEvent.containsKey(val.toString()));
                }
            } else if (event.getKey().startsWith("exact")) {
                Map<String, Object> exact = (Map<String, Object>) event.getValue();
                for (Map.Entry<String, Object> entry : exact.entrySet()) {
                    final String message = "Expected: " + entry.getKey();
                    assertTrue(message, transactionEvent.containsKey(entry.getKey()));
                    final String message1 = "Expected: " + entry.getValue() + " for: " + entry.getKey();
                    assertEquals(message1, entry.getValue(), transactionEvent.get(entry.getKey()));
                }
            } else if (event.getKey().startsWith("unexpected")) {
                for (Object val : (JSONArray) event.getValue()) {
                    assertFalse("Did not expect: " + val.toString() + ", which is " + transactionEvent.get(val.toString()),
                            transactionEvent.containsKey(val.toString()));
                }
            }
        }
    }

    private void assertExpectedMetrics(List metrics, StatsEngine statsEngine) {
        assertNotNull(statsEngine);

        for (Object metric : metrics) {
            List expectedStats = (List) metric;
            String expectedMetricName = (String) expectedStats.get(0);

            Long expectedMetricCount = (Long) ((JSONArray) metric).get(1);
            final String message = String.format("Expected call count %d for: %s", expectedMetricCount, expectedMetricName);
            if (expectedMetricName.startsWith("Supportability") || expectedMetricName.startsWith("ErrorsByCaller")) {
                Stats actualStat = statsEngine.getStats(expectedMetricName);
                assertEquals(message, expectedMetricCount.intValue(), actualStat.getCallCount());
            } else {
                ResponseTimeStats actualStat = statsEngine.getResponseTimeStats(expectedMetricName);
                assertEquals(message, expectedMetricCount.intValue(), actualStat.getCallCount());
            }
        }
    }

    private JSONObject serializeAndParseEvents(TransactionEvent transactionEvent) throws IOException, ParseException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        transactionEvent.writeJSONString(writer);
        writer.flush();

        JSONArray json = (JSONArray) new JSONParser().parse(new String(out.toByteArray()));
        return (JSONObject) json.get(0);
    }

}
