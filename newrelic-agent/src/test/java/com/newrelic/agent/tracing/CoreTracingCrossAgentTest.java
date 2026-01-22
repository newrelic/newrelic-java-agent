/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.MockSpanEventReservoirManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.TransactionState;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.errors.ErrorAnalyzerImpl;
import com.newrelic.agent.errors.ErrorMessageReplacer;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.CollectorSpanEventSender;
import com.newrelic.agent.service.analytics.SpanErrorBuilder;
import com.newrelic.agent.service.analytics.SpanEventCreationDecider;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.service.analytics.TracerToSpanEvent;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.OtherTransSimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionCounts;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoreTracingCrossAgentTest {

    private static final String APP_NAME = "Test";

    private static MockServiceManager serviceManager;
    private static MockRPMService rpmService;

    private static Map<String, Object> defaultConfig;

    private static Instrumentation savedInstrumentation;
    private static com.newrelic.agent.bridge.Agent savedAgent;
    private static ConfigService configService;

    @Mock
    public SpanEventCreationDecider spanEventCreationDecider;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        serviceManager = new MockServiceManager();

        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(AgentConfigImpl.APP_NAME, APP_NAME);
        localSettings.put("distributed_tracing", Collections.singletonMap("enabled", true));
        localSettings.put("span_events", Collections.singletonMap("collect_span_events", true));
        when(spanEventCreationDecider.shouldCreateSpans(any(TransactionData.class))).thenReturn(true);

        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, localSettings, new HashMap<String, Object>());
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, localSettings);
        serviceManager.setConfigService(configService);
        ServiceFactory.setServiceManager(serviceManager);

        serviceManager.setTransactionService(new TransactionService());
        serviceManager.setThreadService(new ThreadService());
        final MockSpanEventReservoirManager reservoirManager = new MockSpanEventReservoirManager(configService);
        Consumer<SpanEvent> backendConsumer = spanEvent -> reservoirManager.getOrCreateReservoir(APP_NAME).add(spanEvent);

        SpanErrorBuilder defaultSpanErrorBuilder = new SpanErrorBuilder(
                new ErrorAnalyzerImpl(agentConfig.getErrorCollectorConfig()),
                new ErrorMessageReplacer(agentConfig.getStripExceptionConfig()));

        Map<String, SpanErrorBuilder> map = new HashMap<>();
        map.put(agentConfig.getApplicationName(), defaultSpanErrorBuilder);

        EnvironmentService environmentService = mock(EnvironmentService.class, RETURNS_DEEP_STUBS);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = mock(TransactionDataToDistributedTraceIntrinsics.class);
        when(transactionDataToDistributedTraceIntrinsics.buildDistributedTracingIntrinsics(any(TransactionData.class), anyBoolean()))
                .thenReturn(Collections.<String, Object>emptyMap());
        TracerToSpanEvent tracerToSpanEvent = new TracerToSpanEvent(map, environmentService, transactionDataToDistributedTraceIntrinsics, defaultSpanErrorBuilder);
        SpanEventsServiceImpl spanEventsService = SpanEventsServiceImpl.builder()
                .agentConfig(agentConfig)
                .reservoirManager(reservoirManager)
                .collectorSender(mock(CollectorSpanEventSender.class))
                .eventBackendStorage(backendConsumer)
                .spanEventCreationDecider(spanEventCreationDecider)
                .tracerToSpanEvent(tracerToSpanEvent)
                .build();
        serviceManager.setSpansEventService(spanEventsService);
        serviceManager.setAttributesService(new AttributesService());
    }

    @After
    public void after() {
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void testPartialGranularitySpans() throws Exception {
        File file = AgentHelper.getFile("com/newrelic/agent/cross_agent_tests/distributed_tracing/partial_granularity.json");
        JSONArray tests = readJsonAndGetArray(file);
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

    private void runTest(String testName, JSONObject jsonTest) throws Exception {
        JSONObject tracerInfo = getTracerInfo(jsonTest);
        String partialGranularityType = (String) jsonTest.get("partial_granularity_type");
        JSONArray expectedSpans = (JSONArray) jsonTest.get("expected_spans");
        JSONArray unexpectedSpans = (JSONArray) jsonTest.get("unexpected_spans");

        Transaction tx = setupTransaction();
        TransactionData transactionData = generateTransactionData(tx, tracerInfo, Transaction.PartialSampleType.valueOf(partialGranularityType.toUpperCase()));

        SpanEventsServiceImpl spanEventsService = (SpanEventsServiceImpl) serviceManager.getSpanEventsService();
        spanEventsService.dispatcherTransactionFinished(transactionData, new TransactionStats());

        SamplingPriorityQueue<SpanEvent> reservoir =  spanEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);

        assertResults(reservoir, expectedSpans, unexpectedSpans);

        // important between tests
        reservoir.clear();
    }

    private void assertResults(SamplingPriorityQueue<SpanEvent> reservoir, JSONArray expectedSpans, JSONArray unexpectedSpans) {
        for (Object obj : expectedSpans) {
            JSONObject jsonObj = (JSONObject) obj;
            String name = (String)jsonObj.keySet().iterator().next();
            JSONObject span = (JSONObject) jsonObj.get(name);
            assertSpanMatching(reservoir, name, span);
        }

        for (Object obj : unexpectedSpans) {
            String name = (String) obj;
            assertFalse(reservoir.asList().stream().anyMatch(reservoirSpan -> name.equals(reservoirSpan.getName())));
        }
    }

    private void assertSpanMatching(SamplingPriorityQueue<SpanEvent> reservoir, String name, JSONObject span) {
        String parent = (String)span.get("parent");
        JSONObject intrinsics = (JSONObject) span.get("intrinsics");
        JSONObject exactIntrinsics = intrinsics == null ? null : (JSONObject) intrinsics.get("exact");
        JSONArray expectedIntrinsics = intrinsics == null ? null : (JSONArray) intrinsics.get("expected");
        JSONArray unexpectedIntrinsics = intrinsics == null ? null : (JSONArray) intrinsics.get("unexpected");
        JSONObject agentAttrs = (JSONObject) span.get("agent_attrs");
        JSONObject exactAgentAttrs = agentAttrs == null ? null : (JSONObject) agentAttrs.get("exact");
        JSONArray expectedAgentAttrs = agentAttrs == null ? null : (JSONArray) agentAttrs.get("expected");
        JSONArray unexpectedAgentAttrs = agentAttrs == null ? null : (JSONArray) agentAttrs.get("unexpected");
        JSONObject userAttrs = (JSONObject) span.get("user_attrs");
        JSONObject exactUserAttrs = userAttrs == null ? null : (JSONObject) userAttrs.get("exact");
        JSONArray expectedUserAttrs = userAttrs == null ? null : (JSONArray) userAttrs.get("expected");
        JSONArray unexpectedUserAttrs = userAttrs == null ? null : (JSONArray) userAttrs.get("unexpected");

        for (SpanEvent reservoirSpan : reservoir.asList()) {
            if (reservoirSpan.getName().equals(name)) {
                if (foundMatchingAttrs(reservoirSpan.getIntrinsics(), exactIntrinsics, expectedIntrinsics, unexpectedIntrinsics)
                        && foundMatchingAttrs(reservoirSpan.getAgentAttributes(), exactAgentAttrs, expectedAgentAttrs, unexpectedAgentAttrs)
                        && foundMatchingAttrs(reservoirSpan.getUserAttributesCopy(), exactUserAttrs, expectedUserAttrs, unexpectedUserAttrs)
                        && foundParentSpan(reservoir, reservoirSpan, parent)) {
                    return;
                }

            }
        }

        fail("No span found matching: "+name);
    }

    private boolean foundMatchingAttrs (Map<String, Object> reservoirSpanAttrs, JSONObject exact, JSONArray expected, JSONArray unexpected) {
        if (exact != null) {
            for (Object exactNameObj : exact.keySet()) {
                String exactName = (String) exactNameObj;
                if (!reservoirSpanAttrs.containsKey(exactName)) {
                    return false;
                }
                if (exact.get(exactName) instanceof Double && reservoirSpanAttrs.get(exactName) instanceof Float) {
                    Double reservoirDouble = Double.valueOf((reservoirSpanAttrs.get(exactName)).toString());
                    if (!reservoirDouble.equals(exact.get(exactName))) {
                        return false;
                    }
                } else {
                    if (!reservoirSpanAttrs.get(exactName).equals(exact.get(exactName))) {
                        return false;
                    }
                }
            }
        }

        if (expected != null) {
            for (Object expectedNameObj : expected) {
                String expectedName = (String) expectedNameObj;
                if (!reservoirSpanAttrs.containsKey(expectedName)) return false;
            }
        }

        if (unexpected != null) {
            for (Object unexpectedNameObj : unexpected) {
                String unexpectedName = (String) unexpectedNameObj;
                if (reservoirSpanAttrs.containsKey(unexpectedName)) return false;
            }
        }

        return true;
    }

    private boolean foundParentSpan(SamplingPriorityQueue<SpanEvent> reservoir, SpanEvent childSpan, String parentName) {
        if (parentName == null) return true;
        for (SpanEvent reservoirSpan : reservoir.asList()) {
            if (reservoirSpan.getName().equals(parentName) && childSpan.getParentId().equals(reservoirSpan.getGuid())) {
                return true;
            }
        }
        return false;
    }

    private TransactionData generateTransactionData(Transaction tx, JSONObject tracerInfo, Transaction.PartialSampleType partialSampleType) throws Exception {
        JSONObject rootTracerInfo = (JSONObject) tracerInfo.get("root_tracer");
        JSONObject childrenFormula = (JSONObject) rootTracerInfo.get("children_formula");

        if (childrenFormula == null) return generateExactTxData(tx, partialSampleType, rootTracerInfo);
        else return generateTxDataViaFormula(tx, partialSampleType, rootTracerInfo, childrenFormula);
    }

    private TransactionData generateTxDataViaFormula(Transaction tx, Transaction.PartialSampleType partialSampleType, JSONObject rootTracerInfo, JSONObject childrenFormula) throws Exception {
        String rootName = (String)rootTracerInfo.get("name");
        Long timestamp = (Long)rootTracerInfo.get("timestamp");
        Long durationMillis = (Long)rootTracerInfo.get("duration_millis");

        Long numChildren = (Long)childrenFormula.get("num_children");
        Long childDurationMillis = (Long)childrenFormula.get("duration_millis");
        Long childDurationGapMillis = (Long)childrenFormula.get("duration_gap_millis");
        String childNamePrefix = (String)childrenFormula.get("name_prefix");
        JSONObject childAgentAttrs = (JSONObject) childrenFormula.get("agent_attrs");

        List<Tracer> tracers = new ArrayList<>();
        DefaultTracer rootTracer = new OtherRootTracer(tx, new ClassMethodSignature("Test", "root", "()V"), this,
                new OtherTransSimpleMetricNameFormat(rootName));
        rootTracer.setMetricName(rootName);
        tx.getTransactionActivity().tracerStarted(rootTracer);

        Long currentTimeMillis = timestamp + childDurationGapMillis;
        for (int i=1;i<=numChildren;i++) {
            DefaultTracer tracer = new DefaultTracer(tx, new ClassMethodSignature("Test", "service B", "()V"), this);
            tracer.setMetricName(childNamePrefix+i);

            addAgentAttrs(tracer, childAgentAttrs);

            tx.getTransactionActivity().tracerStarted(tracer);

            tracer.finish(0, null);
            tracer.setParentTracer(rootTracer);
            overrideTimingData(tracer, currentTimeMillis, childDurationMillis);

            tracers.add(tracer);

            currentTimeMillis += childDurationMillis + childDurationGapMillis;
        }

        rootTracer.finish(0, null);
        overrideTimingData(rootTracer, timestamp, durationMillis);

        return new TransactionDataTestBuilder(
                APP_NAME,
                ServiceFactory.getConfigService().getDefaultAgentConfig(),
                rootTracer)
                .setTx(tx)
                .setTracers(tracers)
                .setPartialSampleType(partialSampleType)
                .build();
    }

    private TransactionData generateExactTxData(Transaction tx, Transaction.PartialSampleType partialSampleType, JSONObject rootTracerInfo) throws Exception {
        List<Tracer> tracers = new ArrayList<>();
        String name = (String)rootTracerInfo.get("name");
        Long timestamp = (Long)rootTracerInfo.get("timestamp");
        Long durationMillis = (Long)rootTracerInfo.get("duration_millis");
        JSONArray children = (JSONArray)rootTracerInfo.get("children");

        DefaultTracer rootTracer = new OtherRootTracer(tx, new ClassMethodSignature("Test", "root", "()V"), this,
                new OtherTransSimpleMetricNameFormat(name));
        rootTracer.setMetricName(name);
        tx.getTransactionActivity().tracerStarted(rootTracer);

        processChildren(rootTracer, children, tx, tracers);

        rootTracer.finish(0, null);
        overrideTimingData(rootTracer, timestamp, durationMillis);

        return new TransactionDataTestBuilder(
                APP_NAME,
                ServiceFactory.getConfigService().getDefaultAgentConfig(),
                rootTracer)
                .setTx(tx)
                .setTracers(tracers)
                .setPartialSampleType(partialSampleType)
                .build();
    }

    private void overrideTimingData(DefaultTracer tracer, Long timestamp, Long durationMillis) throws Exception {
        Field timestampField = DefaultTracer.class.getDeclaredField("timestamp");
        timestampField.setAccessible(true);
        timestampField.set(tracer, timestamp);
        Field startTimeField = DefaultTracer.class.getDeclaredField("startTime");
        startTimeField.setAccessible(true);
        startTimeField.set(tracer, timestamp);
        Field durationField = DefaultTracer.class.getDeclaredField("duration");
        durationField.setAccessible(true);
        durationField.set(tracer, durationMillis*1000000); // to microseconds
    }

    private void processChildren(Tracer parent, JSONArray children, Transaction tx, List<Tracer> tracers) throws Exception {
        if (children == null) return;
        for (Object childObj : children) {
            JSONObject child = (JSONObject) childObj;
            String name = (String) child.get("name");
            Long timestamp = (Long) child.get("timestamp");
            Long durationMillis = (Long) child.get("duration_millis");
            JSONObject agentAttrs = (JSONObject) child.get("agent_attrs");
            JSONObject userAttrs = (JSONObject) child.get("user_attrs");
            JSONArray grandChildren = (JSONArray) child.get("children");

            DefaultTracer tracer = new DefaultTracer(tx, new ClassMethodSignature("Test", "service B", "()V"), this);
            tracer.setMetricName(name);

            addAgentAttrs(tracer, agentAttrs);
            addUserAttrs(tracer, userAttrs);

            tx.getTransactionActivity().tracerStarted(tracer);

            processChildren(tracer, grandChildren, tx, tracers);

            tracer.finish(0, null);
            tracer.setParentTracer(parent);
            overrideTimingData(tracer, timestamp, durationMillis);

            tracers.add(tracer);
        }
    }

    private void addAgentAttrs (Tracer tracer, JSONObject attrs) {
        if (tracer == null || attrs == null) return;
        for (Object attrName : attrs.keySet()) {
            tracer.setAgentAttribute((String)attrName, attrs.get(attrName), true);
        }
    }

    private void addUserAttrs (Tracer tracer, JSONObject attrs) {
        if (tracer == null || attrs == null) return;
        for (Object attrName : attrs.keySet()) {
            Object attrValue = attrs.get(attrName);
            if (attrValue instanceof Number) {
                tracer.addCustomAttribute((String) attrName, (Number)attrs.get(attrName));
            } else if (attrValue instanceof Boolean) {
                tracer.addCustomAttribute((String) attrName, (Boolean)attrs.get(attrName));
            } else {
                tracer.addCustomAttribute((String) attrName, (String)attrs.get(attrName));
            }
        }
    }

    private Transaction setupTransaction() {
        Transaction tx = mock(Transaction.class);

        TransactionActivity txa = mock(TransactionActivity.class);
        when(txa.getTransaction()).thenReturn(tx);
        when(txa.canCreateTransactionSegment()).thenReturn(true);
        when(tx.getTransactionActivity()).thenReturn(txa);

        TransactionState txState = mock(TransactionState.class);
        when(txState.finish(Mockito.any(), Mockito.any())).thenReturn(true);
        when(tx.getTransactionState()).thenReturn(txState);

        TransactionCounts txCounts = mock(TransactionCounts.class);
        when(txCounts.isOverTracerSegmentLimit()).thenReturn(false);
        when(tx.getTransactionCounts()).thenReturn(txCounts);

        when(tx.sampled()).thenReturn(true);
        when(tx.getPriority()).thenReturn(1.5f);

        return tx;
    }

    private JSONObject getTracerInfo(JSONObject jsonTest) throws Exception {
        String fileName = (String)jsonTest.get("tracer_info");
        File file = AgentHelper.getFile("com/newrelic/agent/cross_agent_tests/distributed_tracing/"+fileName);
        return readJsonAndGetObject(file);
    }

    private static JSONArray readJsonAndGetArray(File file) throws Exception {
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        JSONArray theArray = null;
        try {
            fr = new FileReader(file);
            theArray = (JSONArray) parser.parse(fr);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                }
            }
        }
        return theArray;
    }

    private static JSONObject readJsonAndGetObject(File file) throws Exception {
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        JSONObject theObject = null;
        try {
            fr = new FileReader(file);
            theObject = (JSONObject) parser.parse(fr);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                }
            }
        }
        return theObject;
    }
}
