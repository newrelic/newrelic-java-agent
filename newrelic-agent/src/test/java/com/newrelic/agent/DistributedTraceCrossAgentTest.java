/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.attributes.CrossAgentInput;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.api.agent.TransportType;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.DistributedTracingConfig;
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
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.api.agent.DistributedTracePayload;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class DistributedTraceCrossAgentTest {

    private MockServiceManager serviceManager;
    private DistributedTraceServiceImpl distributedTraceService;
    private SpanEventsService spanEventsService;

    private Instrumentation savedInstrumentation;
    private com.newrelic.agent.bridge.Agent savedAgent;

    private final String APP_NAME = "Test";

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Collection<Object[]> getParameters() throws Exception {
        JSONArray tests = CrossAgentInput.readJsonAndGetTests("com/newrelic/agent/cross_agent_tests/distributed_tracing/distributed_tracing.json");

        List<String> testsToSkip = new LinkedList<>();
        // not supported and shouldn't be possible
        testsToSkip.add("high_priority_but_sampled_false");
        // not sure why test expects id but doesn't define one in payload
        testsToSkip.add("payload_from_trusted_partnership_account");

        List<Object[]> parameters = new LinkedList<>();
        for (Object obj : tests) {
            Object test_name = ((JSONObject) obj).get("test_name");

            // Add tests unless they should be skipped
            if (!testsToSkip.contains((String) test_name)) {
                parameters.add(new Object[]{
                        test_name,
                        obj
                });
            }
        }

        return parameters;
    }

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public JSONObject jsonTest;

    private ReservoirManager<SpanEvent> reservoirManager;

    @Before
    public void setUp() throws Exception {
        savedInstrumentation = AgentBridge.instrumentation;
        savedAgent = AgentBridge.agent;

        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, APP_NAME);

        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put("enabled", true);
        config.put("distributed_tracing", dtConfig);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        config.put("span_events", spanConfig);

        ConfigService configService = setupConfig(config);

        distributedTraceService = new DistributedTraceServiceImpl();
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);

        serviceManager.setTransactionTraceService(new TransactionTraceService());
        serviceManager.setTransactionService(new TransactionService());
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));
        serviceManager.setHarvestService(new HarvestServiceImpl());
        serviceManager.setStatsService(new StatsServiceImpl());
        serviceManager.setEnvironmentService(new EnvironmentServiceImpl());
        serviceManager.setAttributesService(new AttributesService());
        AgentBridge.instrumentation = new InstrumentationImpl(Agent.LOG);
        AgentBridge.agent = new AgentImpl(Agent.LOG);

        serviceManager.setCoreService(mock(CoreService.class));
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        reservoirManager = new MockSpanEventReservoirManager(configService);
        spanEventsService = SpanEventsServiceFactory.builder()
                .configService(configService)
                .reservoirManager(reservoirManager)
                .transactionService(serviceManager.getTransactionService())
                .rpmServiceManager(serviceManager.getRPMServiceManager())
                .spanEventCreationDecider(new SpanEventCreationDecider(configService))
                .environmentService(ServiceFactory.getEnvironmentService())
                .transactionDataToDistributedTraceIntrinsics(transactionDataToDistributedTraceIntrinsics)
                .build();
        serviceManager.setDistributedTraceService(distributedTraceService);
        serviceManager.setSpansEventService(spanEventsService);

        ServiceFactory.getServiceManager().start();
    }

    @Test
    public void runTest() throws ParseException, IOException {
        String accountKey = (String) jsonTest.get("trusted_account_key");
        String accountId = (String) jsonTest.get("account_id");
        String transportType = (String) jsonTest.get("transport_type");
        Boolean webTransaction = (Boolean) jsonTest.get("web_transaction");
        Boolean raisesException = (Boolean) jsonTest.get("raises_exception");
        Boolean forceSampledTrue = (Boolean) jsonTest.get("force_sampled_true");
        Long majorVersion = (Long) jsonTest.get("major_version");
        Long minorVersion = (Long) jsonTest.get("minor_version");

        Boolean spanEventsEnabled = (Boolean) jsonTest.get("span_events_enabled");
        replaceConfig(spanEventsEnabled);

        Assert.assertEquals(majorVersion.intValue(), ServiceFactory.getDistributedTraceService().getMajorSupportedCatVersion());
        Assert.assertEquals(minorVersion.intValue(), ServiceFactory.getDistributedTraceService().getMinorSupportedCatVersion());

        JSONArray outbound_payloads = (JSONArray) jsonTest.get("outbound_payloads");
        JSONArray inbound_payloads = (JSONArray) jsonTest.get("inbound_payloads");
        ArrayList expectedMetrics = (ArrayList) jsonTest.get("expected_metrics");

        Map<String, Object> intrinsics = (Map<String, Object>) jsonTest.get("intrinsics");
        Map<String, Object> commonAssertions = (Map<String, Object>) intrinsics.get("common");
        ArrayList targetEvents = (ArrayList) intrinsics.get("target_events");
        Map<String, Object> transactionAssertions = (Map<String, Object>) intrinsics.get("Transaction");
        Map<String, Object> spanAssertions = (Map<String, Object>) intrinsics.get("Span");

        Map<String, Object> connectInfo = new HashMap<>();
        connectInfo.put(DistributedTracingConfig.ACCOUNT_ID, accountId);
        connectInfo.put(DistributedTracingConfig.TRUSTED_ACCOUNT_KEY, accountKey);
        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, Collections.<String, Object>emptyMap(), connectInfo);
        distributedTraceService.connected(null, agentConfig);

        Transaction.clearTransaction();
        TransactionActivity.clear();
        spanEventsService.clearReservoir();

        Transaction tx = Transaction.getTransaction();
        TransactionData transactionData = new TransactionData(tx, 0);
        TransactionStats transactionStats = transactionData.getTransaction().getTransactionActivity().getTransactionStats();

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);

        Tracer rootTracer;
        if (webTransaction) {
            rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "WebTransaction");
        } else {
            rootTracer = TransactionAsyncUtility.createOtherTracer("OtherTransaction");
        }

        tx.getTransactionActivity().tracerStarted(rootTracer);
        if (raisesException) {
            tx.setThrowable(new Throwable(), TransactionErrorPriority.API, false);
        }

        setTransportType(tx, transportType);
        if (inbound_payloads != null) {
            for (Object payload : inbound_payloads) {
                tx.acceptDistributedTracePayload(payload.toString());
            }
        } else {
            tx.acceptDistributedTracePayload((String) null);
        }

        if (forceSampledTrue) {
            tx.setPriorityIfNotNull(new Random().nextFloat() + 1.0f);
        }
        if (outbound_payloads != null) {
            for (Object assertion : outbound_payloads) {
                JSONObject payloadAssertions = (JSONObject) assertion;
                DistributedTracePayload payload = new BoundTransactionApiImpl(tx).createDistributedTracePayload();
                assertOutboundPayload(payloadAssertions, payload);
            }
        }

        rootTracer.finish(Opcodes.RETURN, 0);
        distributedTraceService.dispatcherTransactionFinished(transactionData, transactionStats);
        ((SpanEventsServiceImpl)spanEventsService).dispatcherTransactionFinished(transactionData, transactionStats);
        List<SpanEvent> spans = eventPool.asList();
        TransactionEvent transactionEvent = ServiceFactory.getTransactionEventsService().createEvent(transactionData, transactionStats, "wat");
        JSONObject txnEvents = serializeAndParseEvents(transactionEvent);

        assertExpectedMetrics(expectedMetrics, transactionStats);

        for (Object event : targetEvents) {
            if (event.toString().startsWith("Transaction") && transactionAssertions != null) {
                assertTransactionEvents(transactionAssertions, txnEvents);
                assertTransactionEvents(commonAssertions, txnEvents);
            } else if (event.toString().startsWith("Span") && spanAssertions != null) {
                assertNotEquals("Expected some spans!", 0, spans.size());
                assertSpanEvents(spanAssertions, spans);
                assertSpanEvents(commonAssertions, spans);
            }
        }
    }

    @After
    public void tearDown() {
        AgentBridge.agent = savedAgent;
        AgentBridge.instrumentation = savedInstrumentation;
    }

    private void replaceConfig(boolean spanEventsEnabled) {
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, APP_NAME);

        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put("enabled", true);
        config.put("distributed_tracing", dtConfig);

        Map<String, Object> spansConfig = new HashMap<>();
        spansConfig.put("enabled", spanEventsEnabled);
        spansConfig.put("collect_span_events", true);
        config.put("span_events", spansConfig);

        setupConfig(config);
    }

    private void assertOutboundPayload(JSONObject payloadAssertions, DistributedTracePayload payload) {
        if (payloadAssertions == null) {
            return;
        }

        JSONArray expected = (JSONArray) payloadAssertions.get("expected");
        JSONArray unexpected = (JSONArray) payloadAssertions.get("unexpected");
        JSONObject exact = (JSONObject) payloadAssertions.get("exact");

        JSONObject parsedPayload;

        try {
            parsedPayload = (JSONObject) (new JSONParser()).parse(payload.text());
        } catch (ParseException e) {
            throw new RuntimeException("Unable to parse payload");
        }

        if (expected != null) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) exact).entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("d")) {
                    String dataKey = key.replace("d.", "");
                    JSONObject data = (JSONObject) parsedPayload.get("d");
                    String msg = String.format("Payload %s did not contain key %s value %s", parsedPayload, dataKey, entry.getValue());
                    Assert.assertEquals(msg, entry.getValue(), data.get(dataKey));
                } else if (key.startsWith("v")) {
                    JSONArray expectedVersion = (JSONArray) entry.getValue();
                    final String message = "Expected payload version did not match actual version";
                    Assert.assertEquals(message, expectedVersion, parsedPayload.get("v"));
                } else {
                    Assert.fail("unrecognized key");
                }
            }
        }

        if (expected != null) {
            for (Object key : expected) {
                if (key.toString().startsWith("d")) {
                    String dataKey = key.toString().replace("d.", "");
                    JSONObject data = (JSONObject) parsedPayload.get("d");
                    String msg = String.format("Payload %s did not contain key %s", parsedPayload, dataKey);
                    Assert.assertTrue(msg, data.containsKey(dataKey));
                } else {
                    Assert.fail("unrecognized key");
                }
            }
        }

        if (unexpected != null) {
            for (Object key : unexpected) {
                if (key.toString().startsWith("d")) {
                    String dataKey = key.toString().replace("d.", "");
                    JSONObject data = (JSONObject) parsedPayload.get("d");
                    String msg = String.format("Payload %s did contain key %s", parsedPayload, dataKey);
                    Assert.assertFalse(msg, data.containsKey(dataKey));
                } else {
                    Assert.fail("unrecognized key");
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
                    Assert.assertTrue(intrinsics.containsKey(val.toString()));
                }
            } else if (event.getKey().startsWith("exact")) {
                Map<String, Object> exact = (Map<String, Object>) event.getValue();
                for (Map.Entry<String, Object> entry : exact.entrySet()) {
                    final String message = "Expected: " + entry.getKey();
                    final String message1 = "Expected: " + entry.getValue() + " for: " + entry.getKey();

                    if (entry.getKey().equals("priority")) {
                        // Priority
                        Assert.assertTrue(message, intrinsics.containsKey(entry.getKey()));
                        Double expectedValue = (Double) entry.getValue();
                        Assert.assertEquals(message1, expectedValue.floatValue(), (Float) intrinsics.get(entry.getKey()), 0.0000001f);
                    } else {
                        Assert.assertTrue(message, intrinsics.containsKey(entry.getKey()));
                        Assert.assertEquals(message1, entry.getValue(), intrinsics.get(entry.getKey()));
                    }
                }
            } else if (event.getKey().startsWith("unexpected")) {
                for (Object val : (JSONArray) event.getValue()) {
                    final String message = "Did not expect: " + val.toString();
                    Assert.assertFalse(message, intrinsics.containsKey(val.toString()));
                }
            }
        }
    }

    private void assertTransactionEvents(Map<String, Object> transactionAssertions, JSONObject transactionEvent) {
        for (Map.Entry<String, Object> event : transactionAssertions.entrySet()) {
            if (event.getKey().startsWith("expected")) {
                for (Object val : (JSONArray) event.getValue()) {
                    Assert.assertTrue("Expected: " + val.toString(), transactionEvent.containsKey(val.toString()));
                }
            } else if (event.getKey().startsWith("exact")) {
                Map<String, Object> exact = (Map<String, Object>) event.getValue();
                for (Map.Entry<String, Object> entry : exact.entrySet()) {
                    final String message = "Expected: " + entry.getKey();
                    Assert.assertTrue(message, transactionEvent.containsKey(entry.getKey()));
                    final String message1 = "Expected: " + entry.getValue() + " for: " + entry.getKey();
                    Assert.assertEquals(message1, entry.getValue(), transactionEvent.get(entry.getKey()));
                }
            } else if (event.getKey().startsWith("unexpected")) {
                for (Object val : (JSONArray) event.getValue()) {
                    Assert.assertFalse("Did not expect: " + val.toString(), transactionEvent.containsKey(val.toString()));
                }
            }
        }
    }

    private void assertExpectedMetrics(ArrayList metrics, TransactionStats transactionStats) {
        Assert.assertNotNull(transactionStats);

        for (Object metric : metrics) {
            ArrayList expectedStats = (ArrayList) metric;
            String expectedMetricName = (String) expectedStats.get(0);

            Long expectedMetricCount = (Long) ((JSONArray) metric).get(1);
            final String message = String.format("Expected call count %d for: %s", expectedMetricCount, expectedMetricName);
            if (expectedMetricName.startsWith("Supportability") || expectedMetricName.startsWith("ErrorsByCaller")) {
                Stats actualStat = transactionStats.getUnscopedStats().getStats(expectedMetricName);
                Assert.assertEquals(message, expectedMetricCount.intValue(), actualStat.getCallCount());
            } else {
                ResponseTimeStats actualStat = transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(expectedMetricName);
                Assert.assertEquals(message, expectedMetricCount.intValue(), actualStat.getCallCount());
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

    private ConfigService setupConfig(Map<String, Object> config) {
        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config),
                Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        return configService;
    }
}
