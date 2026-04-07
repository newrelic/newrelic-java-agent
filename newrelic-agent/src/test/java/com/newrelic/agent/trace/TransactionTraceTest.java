/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.OtherTransSimpleMetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionTimer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionTraceTest {

    private AgentConfig iAgentConfig;

    private void setUp(boolean isCaptureAtts, boolean captureRequestAtts, boolean simpleCompression) throws Exception {
        setUp(isCaptureAtts, captureRequestAtts, true, simpleCompression);
    }

    private void setUp(boolean isCaptureAtts, boolean captureRequestAtts, boolean requestUri, boolean simpleCompression) throws Exception {
        iAgentConfig = mock(AgentConfig.class);
        TransactionTracerConfig transTracerConfig = mock(TransactionTracerConfig.class);
        when(iAgentConfig.getTransactionTracerConfig()).thenReturn(transTracerConfig);
        when(iAgentConfig.isSimpleCompression()).thenReturn(simpleCompression);

        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);

        ImmutableMap<String, Object> distributedTracingSettings = ImmutableMap.<String, Object>builder()
                .put(DistributedTracingConfig.ENABLED, Boolean.FALSE)
                .build();

        Map<String, Object> settings = new HashMap<>();
        settings.put(AgentConfigImpl.DISTRIBUTED_TRACING, distributedTracingSettings);
        setConfigAttributes(settings, isCaptureAtts, captureRequestAtts, requestUri, simpleCompression);
        ConfigService cService = new MockConfigService(AgentConfigImpl.createAgentConfig(settings));
        manager.setConfigService(cService);
        manager.setTransactionTraceService(new TransactionTraceService());
        manager.setTransactionService(new TransactionService());
        manager.setAttributesService(new AttributesService());
    }

    private void setConfigAttributes(Map<String, Object> config, boolean captureAtts, boolean captureRequests,
            boolean requestUri, boolean simpleCompression) {
        Map<String, Object> errors = new HashMap<>();
        config.put(AgentConfigImpl.TRANSACTION_TRACER, errors);
        Map<String, Object> atts = new HashMap<>();
        errors.put(AgentConfigImpl.ATTRIBUTES, atts);
        atts.put("enabled", captureAtts);
        if (captureRequests) {
            atts.put("include", "request.*");
        }
        if (!requestUri) {
            atts.put("exclude", "request_uri");
        }
        config.put(AgentConfigImpl.SIMPLE_COMPRESSION_PROPERTY, simpleCompression);
    }

    class TestMethodExitTracer extends MethodExitTracer {

        public TestMethodExitTracer() {
            super(new ClassMethodSignature("Test", "dude", "()V"), Transaction.getTransaction());
        }

        @Override
        protected void doFinish(int opcode, Object returnValue) {
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serializeUserAgentAttsEnabled() throws Exception {
        setUp(true, true, false);
        Tracer rootTracer = new TestMethodExitTracer();
        Map<String, Object> parameters = Collections.emptyMap();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(
                new ClassMethodSignature(getClass().getName(), "testMethod", "testSignature"));
        Mockito.when(tracer.getAgentAttributes()).thenReturn(parameters);
        Mockito.when(tracer.getEndTime()).thenReturn(5000000000L);
        // duration of the tracer is no longer used in the calculation
        Mockito.when(tracer.getDuration()).thenReturn(3000000000L);
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key2", "value2");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key3", "value3");
        long startTime = System.currentTimeMillis();
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";
        String appName = "dude";
        String transactionGuid = "guid";

        TransactionData transactionData = new TransactionDataTestBuilder(appName, iAgentConfig, tracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Collections.singletonList(rootTracer))
                .setRequestParams(requestParams)
                .setUserParams(userParams)
                .setAgentParams(agentParams)
                .setErrorParams(null)
                .setIntrinsics(null)
                .build();

        TransactionTrace trace = TransactionTrace.getTransactionTrace(transactionData,
                SqlObfuscator.getDefaultSqlObfuscator());
        JSONParser parser = new JSONParser();

        // Get the raw data
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(trace);
        // Manually create an "inflated" version for comparison with simple compression
        JSONArray inflatedJsonArray = new JSONArray();
        inflatedJsonArray.addAll(jsonArray);
        Object decodedTTData = decodeTransactionTraceData(inflatedJsonArray.get(4)); // Extract and inflate
        inflatedJsonArray.set(4, decodedTTData); // Store back in array to compare with simple compression below

        Assert.assertNotNull(jsonArray);

        // Second, serialize with simple compression off (data will be deflated)
        byte[] jsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedJsonByteArray = (JSONArray) parser.parse(new String(jsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(jsonArray, parsedJsonByteArray);

        // Third, turn on simple compression and compare with raw data above
        setUp(true, true, true);
        byte[] simpleCompressionJsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedSimpleCompressionJsonByteArray = (JSONArray) parser.parse(new String(
                simpleCompressionJsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(inflatedJsonArray, parsedSimpleCompressionJsonByteArray);

        JSONArray jsonRootSegment = (JSONArray) decodedTTData;

        Assert.assertEquals(8, jsonArray.size());
        Assert.assertEquals(startTime, jsonArray.get(0));
        Assert.assertEquals(5000L, jsonArray.get(1));
        Assert.assertEquals(frontendMetricName, jsonArray.get(2));
        Assert.assertEquals(requestUri, jsonArray.get(3));
        Assert.assertEquals(transactionGuid, jsonArray.get(5));
        Assert.assertNull(jsonArray.get(6));
        Assert.assertEquals(false, jsonArray.get(7)); // obsolete forcePersist flag
        Assert.assertEquals(5, jsonRootSegment.size());

        Assert.assertEquals(startTime, jsonRootSegment.get(0));
        JSONObject emptyObject = (JSONObject) jsonRootSegment.get(1);
        Assert.assertEquals(0, emptyObject.size());

        emptyObject = (JSONObject) jsonRootSegment.get(2);
        Assert.assertEquals(0, emptyObject.size());

        JSONObject atts = (JSONObject) jsonRootSegment.get(4);
        Assert.assertEquals(3, atts.size());
        Map<String, Object> innerAtts = (Map<String, Object>) atts.get("agentAttributes");
        Assert.assertEquals(3, innerAtts.size());
        Assert.assertEquals("value1", innerAtts.get("request.parameters.key1"));
        Assert.assertEquals("value3", innerAtts.get("key3"));

        innerAtts = (Map<String, Object>) atts.get("userAttributes");
        Assert.assertEquals(1, innerAtts.size());
        Assert.assertEquals("value2", innerAtts.get("key2"));

        innerAtts = (Map<String, Object>) atts.get("intrinsics");
        Assert.assertEquals(1, innerAtts.size());
        Assert.assertNotNull(innerAtts.get("totalTime"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serializeUserAgentAttsEnabledRequestUriCanNotBeDisabled() throws Exception {
        setUp(true, true, false, false);
        Tracer rootTracer = new TestMethodExitTracer();
        Map<String, Object> parameters = Collections.emptyMap();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(
                new ClassMethodSignature(getClass().getName(), "testMethod", "testSignature"));
        Mockito.when(tracer.getAgentAttributes()).thenReturn(parameters);
        Mockito.when(tracer.getEndTime()).thenReturn(5000000000L);
        Mockito.when(tracer.getDuration()).thenReturn(3000000000L);
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key2", "value2");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key3", "value3");
        long startTime = System.currentTimeMillis();
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";
        String appName = "dude";
        String transactionGuid = "guid";

        TransactionData transactionData = new TransactionDataTestBuilder(appName, iAgentConfig, tracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Collections.singletonList(rootTracer))
                .setRequestParams(requestParams)
                .setUserParams(userParams)
                .setAgentParams(agentParams)
                .setErrorParams(null)
                .setIntrinsics(null)
                .build();

        TransactionTrace trace = TransactionTrace.getTransactionTrace(transactionData,
                SqlObfuscator.getDefaultSqlObfuscator());
        JSONParser parser = new JSONParser();

        // Get the raw data
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(trace);
        // Manually create an "inflated" version for comparison with simple compression
        JSONArray inflatedJsonArray = new JSONArray();
        inflatedJsonArray.addAll(jsonArray);
        Object decodedTTData = decodeTransactionTraceData(inflatedJsonArray.get(4)); // Extract and inflate
        inflatedJsonArray.set(4, decodedTTData); // Store back in array to compare with simple compression below

        Assert.assertNotNull(jsonArray);

        // Second, serialize with simple compression off (data will be deflated)
        byte[] jsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedJsonByteArray = (JSONArray) parser.parse(new String(jsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(jsonArray, parsedJsonByteArray);

        // Third, turn on simple compression and compare with raw data above
        setUp(true, true, false, true);
        byte[] simpleCompressionJsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedSimpleCompressionJsonByteArray = (JSONArray) parser.parse(new String(
                simpleCompressionJsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(inflatedJsonArray, parsedSimpleCompressionJsonByteArray);

        JSONArray jsonRootSegment = (JSONArray) decodedTTData;

        Assert.assertEquals(8, jsonArray.size());
        Assert.assertEquals(startTime, jsonArray.get(0));
        Assert.assertEquals(5000L, jsonArray.get(1));
        Assert.assertEquals(frontendMetricName, jsonArray.get(2));
        Assert.assertEquals(requestUri, jsonArray.get(3));
        Assert.assertEquals(transactionGuid, jsonArray.get(5));
        Assert.assertNull(jsonArray.get(6));
        Assert.assertEquals(false, jsonArray.get(7)); // obsolete forcePersist flag
        Assert.assertEquals(5, jsonRootSegment.size());

        Assert.assertEquals(startTime, jsonRootSegment.get(0));
        JSONObject emptyObject = (JSONObject) jsonRootSegment.get(1);
        Assert.assertEquals(0, emptyObject.size());

        emptyObject = (JSONObject) jsonRootSegment.get(2);
        Assert.assertEquals(0, emptyObject.size());

        JSONObject atts = (JSONObject) jsonRootSegment.get(4);
        Assert.assertEquals(3, atts.size());
        Map<String, Object> innerAtts = (Map<String, Object>) atts.get("agentAttributes");
        Assert.assertEquals(3, innerAtts.size());
        Assert.assertEquals("value1", innerAtts.get("request.parameters.key1"));
        Assert.assertEquals("value3", innerAtts.get("key3"));

        innerAtts = (Map<String, Object>) atts.get("userAttributes");
        Assert.assertEquals(1, innerAtts.size());
        Assert.assertEquals("value2", innerAtts.get("key2"));

        innerAtts = (Map<String, Object>) atts.get("intrinsics");
        Assert.assertEquals(1, innerAtts.size());
        Assert.assertNotNull(innerAtts.get("totalTime"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serializeUserAgentIntrinsicsEnabled() throws Exception {
        setUp(true, true, false);
        Tracer rootTracer = new TestMethodExitTracer();
        Map<String, Object> parameters = Collections.emptyMap();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(
                new ClassMethodSignature(getClass().getName(), "testMethod", "testSignature"));
        Mockito.when(tracer.getAgentAttributes()).thenReturn(parameters);
        Mockito.when(tracer.getEndTime()).thenReturn(5000000000L);
        Mockito.when(tracer.getDuration()).thenReturn(3000000000L);

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key2", "value2");
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("error1", "valueError");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key3", "value3");
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key4", "value4");
        intrinsics.put("key5", 5L);
        long startTime = System.currentTimeMillis();
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";
        String appName = "dude";
        String transactionGuid = "guid";

        TransactionData transactionData = new TransactionDataTestBuilder(appName, iAgentConfig, tracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Collections.singletonList(rootTracer))
                .setRequestParams(requestParams)
                .setUserParams(userParams)
                .setAgentParams(agentParams)
                .setErrorParams(errorParams)
                .setIntrinsics(intrinsics)
                .build();

        TransactionTrace trace = TransactionTrace.getTransactionTrace(transactionData,
                SqlObfuscator.getDefaultSqlObfuscator());
        JSONParser parser = new JSONParser();

        // Get the raw data
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(trace);
        // Manually create an "inflated" version for comparison with simple compression
        JSONArray inflatedJsonArray = new JSONArray();
        inflatedJsonArray.addAll(jsonArray);
        Object decodedTTData = decodeTransactionTraceData(inflatedJsonArray.get(4)); // Extract and inflate
        inflatedJsonArray.set(4, decodedTTData); // Store back in array to compare with simple compression below

        Assert.assertNotNull(jsonArray);

        // Second, serialize with simple compression off (data will be deflated)
        byte[] jsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedJsonByteArray = (JSONArray) parser.parse(new String(jsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(jsonArray, parsedJsonByteArray);

        // Third, turn on simple compression and compare with raw data above
        setUp(true, true, true);
        byte[] simpleCompressionJsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedSimpleCompressionJsonByteArray = (JSONArray) parser.parse(new String(
                simpleCompressionJsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(inflatedJsonArray, parsedSimpleCompressionJsonByteArray);

        JSONArray jsonRootSegment = (JSONArray) decodedTTData;

        long txDurMs = transactionData.getTransactionTime().getTransactionDurationInMilliseconds();
        long responseDurMs = transactionData.getTransactionTime().getResponseTimeInMilliseconds();
        System.out.println(responseDurMs);
        System.out.println(txDurMs);
        Assert.assertEquals(8, jsonArray.size());
        Assert.assertEquals(startTime, jsonArray.get(0));
        Assert.assertEquals(5000L, jsonArray.get(1));
        Assert.assertEquals(frontendMetricName, jsonArray.get(2));
        Assert.assertEquals(requestUri, jsonArray.get(3));
        Assert.assertEquals(transactionGuid, jsonArray.get(5));
        Assert.assertNull(jsonArray.get(6));
        Assert.assertEquals(false, jsonArray.get(7)); // obsolete forcePersist flag
        Assert.assertEquals(5, jsonRootSegment.size());

        Assert.assertEquals(startTime, jsonRootSegment.get(0));
        JSONObject emptyObject = (JSONObject) jsonRootSegment.get(1);
        Assert.assertEquals(0, emptyObject.size());
        emptyObject = (JSONObject) jsonRootSegment.get(2);
        Assert.assertEquals(0, emptyObject.size());

        JSONObject atts = (JSONObject) jsonRootSegment.get(4);
        Assert.assertEquals(3, atts.size());
        Map<String, Object> innerAtts = (Map<String, Object>) atts.get("agentAttributes");
        Assert.assertEquals(3, innerAtts.size());
        Assert.assertEquals("value1", innerAtts.get("request.parameters.key1"));
        Assert.assertEquals("value3", innerAtts.get("key3"));

        innerAtts = (Map<String, Object>) atts.get("userAttributes");
        Assert.assertEquals(1, innerAtts.size());
        Assert.assertEquals("value2", innerAtts.get("key2"));

        innerAtts = (Map<String, Object>) atts.get("intrinsics");
        Assert.assertEquals(3, innerAtts.size());
        Assert.assertEquals("value4", innerAtts.get("key4"));
        Assert.assertEquals(5L, innerAtts.get("key5"));
        Assert.assertNotNull(innerAtts.get("totalTime"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serializeUserOnlyEnabledRequestAttsDisabled() throws Exception {
        setUp(true, false, false);
        Tracer rootTracer = new TestMethodExitTracer();
        Map<String, Object> parameters = Collections.emptyMap();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(
                new ClassMethodSignature(getClass().getName(), "testMethod", "testSignature"));
        Mockito.when(tracer.getAgentAttributes()).thenReturn(parameters);
        long rootTracerDurationInMilliseconds = 100L;
        Mockito.when(tracer.getDurationInMilliseconds()).thenReturn(rootTracerDurationInMilliseconds);
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key3", "value3");
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key4", "value4");
        intrinsics.put("key5", 5L);
        long startTime = System.currentTimeMillis();
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";
        String appName = "dude";

        TransactionData transactionData = new TransactionDataTestBuilder(appName, iAgentConfig, tracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Collections.singletonList(rootTracer))
                .setRequestParams(requestParams)
                .setAgentParams(agentParams)
                .setIntrinsics(intrinsics)
                .build();

        TransactionTrace trace = TransactionTrace.getTransactionTrace(transactionData,
                SqlObfuscator.getDefaultSqlObfuscator());
        JSONParser parser = new JSONParser();

        // Get the raw data
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(trace);
        // Manually create an "inflated" version for comparison with simple compression
        JSONArray inflatedJsonArray = new JSONArray();
        inflatedJsonArray.addAll(jsonArray);
        Object decodedTTData = decodeTransactionTraceData(inflatedJsonArray.get(4)); // Extract and inflate
        inflatedJsonArray.set(4, decodedTTData); // Store back in array to compare with simple compression below

        Assert.assertNotNull(jsonArray);

        // Second, serialize with simple compression off (data will be deflated)
        byte[] jsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedJsonByteArray = (JSONArray) parser.parse(new String(jsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(jsonArray, parsedJsonByteArray);

        // Third, turn on simple compression and compare with raw data above
        setUp(true, false, true);
        byte[] simpleCompressionJsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedSimpleCompressionJsonByteArray = (JSONArray) parser.parse(new String(
                simpleCompressionJsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(inflatedJsonArray, parsedSimpleCompressionJsonByteArray);

        JSONArray jsonRootSegment = (JSONArray) decodedTTData;

        Assert.assertEquals(requestUri, jsonArray.get(3));
        Assert.assertEquals(5, jsonRootSegment.size());

        JSONObject atts = (JSONObject) jsonRootSegment.get(4);
        Assert.assertEquals(2, atts.size());
        Map<String, Object> innerAtts = (Map<String, Object>) atts.get("agentAttributes");
        Assert.assertEquals(2, innerAtts.size());
        Assert.assertEquals("value3", innerAtts.get("key3"));

        innerAtts = (Map<String, Object>) atts.get("intrinsics");
        Assert.assertEquals(3, innerAtts.size());
        Assert.assertEquals("value4", innerAtts.get("key4"));
        Assert.assertEquals(5L, innerAtts.get("key5"));
        Assert.assertNotNull(innerAtts.get("totalTime"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serializeAgentOnlyEnabled() throws Exception {
        setUp(true, true, false);
        Tracer rootTracer = new TestMethodExitTracer();
        Map<String, Object> parameters = Collections.emptyMap();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(
                new ClassMethodSignature(getClass().getName(), "testMethod", "testSignature"));
        Mockito.when(tracer.getAgentAttributes()).thenReturn(parameters);
        long rootTracerDurationInMilliseconds = 100L;
        Mockito.when(tracer.getDurationInMilliseconds()).thenReturn(rootTracerDurationInMilliseconds);
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key3", "value3");
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key4", "value4");
        intrinsics.put("key5", 5L);
        long startTime = System.currentTimeMillis();
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";
        String appName = "dude";

        TransactionData transactionData = new TransactionDataTestBuilder(appName, iAgentConfig, tracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Collections.singletonList(rootTracer))
                .setRequestParams(requestParams)
                .setAgentParams(agentParams)
                .setIntrinsics(intrinsics)
                .build();

        TransactionTrace trace = TransactionTrace.getTransactionTrace(transactionData,
                SqlObfuscator.getDefaultSqlObfuscator());
        JSONParser parser = new JSONParser();

        // Get the raw data
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(trace);
        // Manually create an "inflated" version for comparison with simple compression
        JSONArray inflatedJsonArray = new JSONArray();
        inflatedJsonArray.addAll(jsonArray);
        Object decodedTTData = decodeTransactionTraceData(inflatedJsonArray.get(4)); // Extract and inflate
        inflatedJsonArray.set(4, decodedTTData); // Store back in array to compare with simple compression below

        Assert.assertNotNull(jsonArray);

        // Second, serialize with simple compression off (data will be deflated)
        byte[] jsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedJsonByteArray = (JSONArray) parser.parse(new String(jsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(jsonArray, parsedJsonByteArray);

        // Third, turn on simple compression and compare with raw data above
        setUp(true, true, true);
        byte[] simpleCompressionJsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedSimpleCompressionJsonByteArray = (JSONArray) parser.parse(new String(
                simpleCompressionJsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(inflatedJsonArray, parsedSimpleCompressionJsonByteArray);

        JSONArray jsonRootSegment = (JSONArray) decodedTTData;

        Assert.assertEquals(requestUri, jsonArray.get(3));
        Assert.assertEquals(5, jsonRootSegment.size());

        JSONObject atts = (JSONObject) jsonRootSegment.get(4);
        Assert.assertEquals(2, atts.size());
        Map<String, Object> innerAtts = (Map<String, Object>) atts.get("agentAttributes");
        Assert.assertEquals(3, innerAtts.size());
        Assert.assertEquals("value1", innerAtts.get("request.parameters.key1"));
        Assert.assertEquals("value3", innerAtts.get("key3"));

        innerAtts = (Map<String, Object>) atts.get("intrinsics");
        Assert.assertEquals(3, innerAtts.size());
        Assert.assertEquals("value4", innerAtts.get("key4"));
        Assert.assertEquals(5L, innerAtts.get("key5"));
        Assert.assertNotNull(innerAtts.get("totalTime"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serializeUserOnlyEnabled() throws Exception {
        setUp(true, true, false);
        Tracer rootTracer = new TestMethodExitTracer();
        Map<String, Object> parameters = Collections.emptyMap();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(
                new ClassMethodSignature(getClass().getName(), "testMethod", "testSignature"));
        Mockito.when(tracer.getAgentAttributes()).thenReturn(parameters);
        long rootTracerDurationInMilliseconds = 100L;
        Mockito.when(tracer.getDurationInMilliseconds()).thenReturn(rootTracerDurationInMilliseconds);
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key2", "value2");
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("error1", "valueError");
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key4", "value4");
        intrinsics.put("key5", 5L);
        long startTime = System.currentTimeMillis();
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";
        String appName = "dude";

        TransactionData transactionData = new TransactionDataTestBuilder(appName, iAgentConfig, tracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Collections.singletonList(rootTracer))
                .setRequestParams(null)
                .setUserParams(userParams)
                .setAgentParams(null)
                .setErrorParams(errorParams)
                .setIntrinsics(intrinsics)
                .build();

        TransactionTrace trace = TransactionTrace.getTransactionTrace(transactionData,
                SqlObfuscator.getDefaultSqlObfuscator());
        JSONParser parser = new JSONParser();

        // Get the raw data
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(trace);
        // Manually create an "inflated" version for comparison with simple compression
        JSONArray inflatedJsonArray = new JSONArray();
        inflatedJsonArray.addAll(jsonArray);
        Object decodedTTData = decodeTransactionTraceData(inflatedJsonArray.get(4)); // Extract and inflate
        inflatedJsonArray.set(4, decodedTTData); // Store back in array to compare with simple compression below

        Assert.assertNotNull(jsonArray);

        // Second, serialize with simple compression off (data will be deflated)
        byte[] jsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedJsonByteArray = (JSONArray) parser.parse(new String(jsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(jsonArray, parsedJsonByteArray);

        // Third, turn on simple compression and compare with raw data above
        setUp(true, true, true);
        byte[] simpleCompressionJsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedSimpleCompressionJsonByteArray = (JSONArray) parser.parse(new String(
                simpleCompressionJsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(inflatedJsonArray, parsedSimpleCompressionJsonByteArray);

        JSONArray jsonRootSegment = (JSONArray) decodedTTData;

        // request uri
        Assert.assertEquals(requestUri, jsonArray.get(3));

        Assert.assertEquals(5, jsonRootSegment.size());

        JSONObject atts = (JSONObject) jsonRootSegment.get(4);
        Assert.assertEquals(3, atts.size());
        Map<String, Object> innerAtts = (Map<String, Object>) atts.get("userAttributes");
        Assert.assertEquals(1, innerAtts.size());
        Assert.assertEquals("value2", innerAtts.get("key2"));

        innerAtts = (Map<String, Object>) atts.get("intrinsics");
        Assert.assertEquals(3, innerAtts.size());
        Assert.assertEquals("value4", innerAtts.get("key4"));
        Assert.assertEquals(5L, innerAtts.get("key5"));
        Assert.assertNotNull(innerAtts.get("totalTime"));
    }

    @Test
    public void serializeAttsDisabled() throws Exception {
        setUp(false, true, false);
        Tracer rootTracer = new TestMethodExitTracer();
        Map<String, Object> parameters = Collections.emptyMap();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(
                new ClassMethodSignature(getClass().getName(), "testMethod", "testSignature"));
        Mockito.when(tracer.getAgentAttributes()).thenReturn(parameters);
        Mockito.when(tracer.getEndTime()).thenReturn(5000000000L);
        Mockito.when(tracer.getDuration()).thenReturn(3000000000L);
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key2", "value2");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key3", "value3");
        long startTime = System.currentTimeMillis();
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";
        String appName = "dude";
        String transactionGuid = "guid";

        TransactionData transactionData = new TransactionDataTestBuilder(appName, iAgentConfig, tracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Collections.singletonList(rootTracer))
                .setRequestParams(requestParams)
                .setUserParams(userParams)
                .setAgentParams(agentParams)
                .setErrorParams(null)
                .setIntrinsics(null)
                .build();

        TransactionTrace trace = TransactionTrace.getTransactionTrace(transactionData,
                SqlObfuscator.getDefaultSqlObfuscator());
        JSONParser parser = new JSONParser();

        // Get the raw data
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(trace);
        // Manually create an "inflated" version for comparison with simple compression
        JSONArray inflatedJsonArray = new JSONArray();
        inflatedJsonArray.addAll(jsonArray);
        Object decodedTTData = decodeTransactionTraceData(inflatedJsonArray.get(4)); // Extract and inflate
        inflatedJsonArray.set(4, decodedTTData); // Store back in array to compare with simple compression below

        Assert.assertNotNull(jsonArray);

        // Second, serialize with simple compression off (data will be deflated)
        byte[] jsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedJsonByteArray = (JSONArray) parser.parse(new String(jsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(jsonArray, parsedJsonByteArray);

        // Third, turn on simple compression and compare with raw data above
        setUp(false, true, true);
        byte[] simpleCompressionJsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedSimpleCompressionJsonByteArray = (JSONArray) parser.parse(new String(
                simpleCompressionJsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(inflatedJsonArray, parsedSimpleCompressionJsonByteArray);

        JSONArray jsonRootSegment = (JSONArray) decodedTTData;

        Assert.assertEquals(8, jsonArray.size());
        Assert.assertEquals(startTime, jsonArray.get(0));
        Assert.assertEquals(5000L, jsonArray.get(1));
        Assert.assertEquals(frontendMetricName, jsonArray.get(2));

        Object serializedRequestUri = jsonArray.get(3);
        Assert.assertEquals(null, serializedRequestUri);

        Assert.assertEquals(transactionGuid, jsonArray.get(5));
        Assert.assertNull(jsonArray.get(6));
        Assert.assertEquals(false, jsonArray.get(7)); // obsolete forcePersist flag
        Assert.assertEquals(5, jsonRootSegment.size());
        Assert.assertEquals(startTime, jsonRootSegment.get(0));
        JSONObject emptyObject = (JSONObject) jsonRootSegment.get(1);
        Assert.assertEquals(0, emptyObject.size());
        emptyObject = (JSONObject) jsonRootSegment.get(2);
        Assert.assertEquals(0, emptyObject.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serializeAttsDisabledWithIntrinsics() throws Exception {
        setUp(false, true, false);
        Tracer rootTracer = new TestMethodExitTracer();
        Map<String, Object> parameters = Collections.emptyMap();
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(
                new ClassMethodSignature(getClass().getName(), "testMethod", "testSignature"));
        Mockito.when(tracer.getAgentAttributes()).thenReturn(parameters);
        Mockito.when(tracer.getEndTime()).thenReturn(5000000000L);
        Mockito.when(tracer.getDuration()).thenReturn(3000000000L);
        ;
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key2", "value2");
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("error1", "valueError");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key3", "value3");
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key4", "value4");
        intrinsics.put("key5", 5L);
        long startTime = System.currentTimeMillis();
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";
        String appName = "dude";
        String transactionGuid = "guid";

        TransactionData transactionData = new TransactionDataTestBuilder(appName, iAgentConfig, tracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Collections.singletonList(rootTracer))
                .setRequestParams(requestParams)
                .setUserParams(userParams)
                .setAgentParams(agentParams)
                .setErrorParams(errorParams)
                .setIntrinsics(intrinsics)
                .build();

        TransactionTrace trace = TransactionTrace.getTransactionTrace(transactionData,
                SqlObfuscator.getDefaultSqlObfuscator());
        JSONParser parser = new JSONParser();

        // Get the raw data
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(trace);
        // Manually create an "inflated" version for comparison with simple compression
        JSONArray inflatedJsonArray = new JSONArray();
        inflatedJsonArray.addAll(jsonArray);
        Object decodedTTData = decodeTransactionTraceData(inflatedJsonArray.get(4)); // Extract and inflate
        inflatedJsonArray.set(4, decodedTTData); // Store back in array to compare with simple compression below

        Assert.assertNotNull(jsonArray);

        // Second, serialize with simple compression off (data will be deflated)
        byte[] jsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedJsonByteArray = (JSONArray) parser.parse(new String(jsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(jsonArray, parsedJsonByteArray);

        // Third, turn on simple compression and compare with raw data above
        setUp(false, true, true);
        byte[] simpleCompressionJsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedSimpleCompressionJsonByteArray = (JSONArray) parser.parse(new String(
                simpleCompressionJsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(inflatedJsonArray, parsedSimpleCompressionJsonByteArray);

        JSONArray jsonRootSegment = (JSONArray) decodedTTData;

        Assert.assertEquals(8, jsonArray.size());
        Assert.assertEquals(startTime, jsonArray.get(0));
        Assert.assertEquals(5000L, jsonArray.get(1));
        Assert.assertEquals(frontendMetricName, jsonArray.get(2));

        Object serializedRequestUri = jsonArray.get(3);
        Assert.assertEquals(null, serializedRequestUri);

        Assert.assertEquals(transactionGuid, jsonArray.get(5));
        Assert.assertNull(jsonArray.get(6));
        Assert.assertEquals(false, jsonArray.get(7)); // obsolete forcePersist flag
        Assert.assertEquals(5, jsonRootSegment.size());
        Assert.assertEquals(startTime, jsonRootSegment.get(0));
        JSONObject emptyObject = (JSONObject) jsonRootSegment.get(1);
        Assert.assertEquals(0, emptyObject.size());
        emptyObject = (JSONObject) jsonRootSegment.get(2);
        Assert.assertEquals(0, emptyObject.size());

        JSONObject atts = (JSONObject) jsonRootSegment.get(4);
        Assert.assertEquals(1, atts.size());

        Map<String, Object> innerAtts = (Map<String, Object>) atts.get("intrinsics");
        Assert.assertEquals(3, innerAtts.size());
        Assert.assertEquals("value4", innerAtts.get("key4"));
        Assert.assertEquals(5L, innerAtts.get("key5"));
        Assert.assertNotNull(innerAtts.get("totalTime"));

    }

    @Test
    public void testExecContext() throws Exception {
        setUp(false, true, false);
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = new OtherRootTracer(tx, new ClassMethodSignature("Test", "root", "()V"), this,
                new OtherTransSimpleMetricNameFormat("myMetricName"));
        DefaultTracer tracer1 = new DefaultTracer(tx, new ClassMethodSignature("Test", "dude", "()V"), this);
        DefaultTracer tracer2 = new DefaultTracer(tx, new ClassMethodSignature("Test", "dude2", "()V"), this);

        tx.getTransactionActivity().tracerStarted(rootTracer);
        tx.getTransactionActivity().tracerStarted(tracer1);
        tx.getTransactionActivity().tracerStarted(tracer2);
        tracer2.finish(0, null);
        tracer1.finish(0, null);
        rootTracer.finish(0, null);

        long startTime = System.currentTimeMillis();
        String frontendMetricName = "Frontend/dude";
        String requestUri = "/dude";
        String appName = "dude";

        TransactionData transactionData = new TransactionDataTestBuilder(appName, iAgentConfig, rootTracer)
                .setStartTime(startTime)
                .setRequestUri(requestUri)
                .setFrontendMetricName(frontendMetricName)
                .setTracers(Arrays.asList(rootTracer, tracer1, tracer2))
                .build();

        TransactionTrace trace = TransactionTrace.getTransactionTrace(transactionData,
                SqlObfuscator.getDefaultSqlObfuscator());
        JSONParser parser = new JSONParser();

        // Get the raw data
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(trace);
        // Manually create an "inflated" version for comparison with simple compression
        JSONArray inflatedJsonArray = new JSONArray();
        inflatedJsonArray.addAll(jsonArray);
        Object decodedTTData = decodeTransactionTraceData(inflatedJsonArray.get(4)); // Extract and inflate
        inflatedJsonArray.set(4, decodedTTData); // Store back in array to compare with simple compression below

        Assert.assertNotNull(jsonArray);

        // Second, serialize with simple compression off (data will be deflated)
        byte[] jsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedJsonByteArray = (JSONArray) parser.parse(new String(jsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(jsonArray, parsedJsonByteArray);

        // Third, turn on simple compression and compare with raw data above
        setUp(false, true, true);
        byte[] simpleCompressionJsonByteArray = AgentHelper.serializeJSONToByteArray(trace);
        JSONArray parsedSimpleCompressionJsonByteArray = (JSONArray) parser.parse(new String(
                simpleCompressionJsonByteArray, StandardCharsets.UTF_8));
        Assert.assertEquals(inflatedJsonArray, parsedSimpleCompressionJsonByteArray);

        JSONArray jsonRootSegment = (JSONArray) decodedTTData;

        Assert.assertEquals(8, jsonArray.size());

        JSONArray rootSeg = (JSONArray) jsonRootSegment.get(3);
        Assert.assertNotNull(rootSeg);
        verifyAllTraceSegmentsHaveExecContext(rootSeg);
    }

    private void verifyAllTraceSegmentsHaveExecContext(JSONArray root) {
        JSONObject params = (JSONObject) root.get(3);
        Assert.assertNotNull(params);
        Assert.assertNotNull(params.get("async_context"));
        Assert.assertNotNull(params.get("exclusive_duration_millis"));
        Assert.assertEquals(Thread.currentThread().getName(), params.get("async_context"));
        JSONArray children = (JSONArray) root.get(4);
        Assert.assertNotNull(children);
        Assert.assertEquals(1, children.size());
        JSONArray child = (JSONArray) children.get(0);
        JSONObject childParams = (JSONObject) child.get(3);
        Assert.assertEquals(Thread.currentThread().getName(), childParams.get("async_context"));
        Assert.assertNotNull(childParams.get("exclusive_duration_millis"));
    }

    @Test
    public void testGetMetricName() throws Exception {
        setUp(true, true, false);
        DefaultTracer tracer = new DefaultTracer(Transaction.getTransaction(), new ClassMethodSignature("Test", "dude",
                "()V"), this, new SimpleMetricNameFormat("hello"));
        Assert.assertEquals("hello", TransactionSegment.getMetricName(tracer));

        tracer = new DefaultTracer(Transaction.getTransaction(), new ClassMethodSignature("Test", "dude", "()V"), this,
                new SimpleMetricNameFormat("hello", "segmentName"));
        Assert.assertEquals("segmentName", TransactionSegment.getMetricName(tracer));

        tracer = new DefaultTracer(Transaction.getTransaction(), new ClassMethodSignature("Test", "dude", "()V"), this,
                new SimpleMetricNameFormat("hello", "1234"));
        Assert.assertEquals("1234", TransactionSegment.getMetricName(tracer));

        // Writing this test based on the current code. No clue why we would ever want a segment named after the
        // tracer. This test proves that you can not get a null transaction segment metric name.
        tracer = new DefaultTracer(Transaction.getTransaction(), new ClassMethodSignature("Test", "dude", "()V"), this,
                new SimpleMetricNameFormat("hello", ""));
        Assert.assertEquals("com.newrelic.agent.tracers.DefaultTracer*", TransactionSegment.getMetricName(tracer));

        tracer = new DefaultTracer(Transaction.getTransaction(), new ClassMethodSignature("Test", "dude", "()V"), this,
                new SimpleMetricNameFormat("hello", null));
        Assert.assertEquals("com.newrelic.agent.tracers.DefaultTracer*", TransactionSegment.getMetricName(tracer));

        tracer = new DefaultTracer(Transaction.getTransaction(), new ClassMethodSignature("Test", "dude", "()V"), this,
                new SimpleMetricNameFormat(null, null));
        Assert.assertEquals("com.newrelic.agent.tracers.DefaultTracer*", TransactionSegment.getMetricName(tracer));

        tracer = new DefaultTracer(Transaction.getTransaction(), new ClassMethodSignature("Test", "dude", "()V"), this,
                new SimpleMetricNameFormat("hello", "           "));
        Assert.assertEquals("com.newrelic.agent.tracers.DefaultTracer*", TransactionSegment.getMetricName(tracer));
    }

    /**
     * This test exercises an edge case in our public tracer api by setting excludeFromTransactionTrace = true on a root
     * tracer.
     *
     * The behavior is undefined, but in practice only the root tracer's segment is preserved in the TT. Scala
     * instrumentation is relying on this behavior by only showing the root scala future but not the consecutive calls.
     */
    @Test
    public void excludeRootTraceFromTT() throws Exception {
        setUp(true, true, false);
        Transaction tx = Transaction.getTransaction(true);
        TransactionActivity txa = TransactionActivity.get();
        // make sure the test env is sane
        Assert.assertNotNull(tx);
        Assert.assertNotNull(txa);
        Assert.assertEquals(tx, txa.getTransaction());

        final ClassMethodSignature sig = new ClassMethodSignature("Test", "dude", "()V");
        final MetricNameFormat metricNameFormat = new SimpleMetricNameFormat("Test.dude", "Test.dude");
        final int excludeFromTTFlags = TracerFlags.GENERATE_SCOPED_METRIC; // @Trace( excludeFromTransactionTrace = true )
        final int defaultFlags = TracerFlags.GENERATE_SCOPED_METRIC | TracerFlags.TRANSACTION_TRACER_SEGMENT; // @Trace

        DefaultTracer root = new OtherRootTracer(txa, sig, new Object(), metricNameFormat, excludeFromTTFlags,
                System.currentTimeMillis());
        txa.addTracer(root);
        Assert.assertEquals(txa, root.getTransactionActivity());
        Assert.assertEquals(tx, root.getTransaction());

        final int numChildren = 10;
        for (int i = 0; i < numChildren; ++i) {
            DefaultTracer child = new DefaultTracer(txa, sig, new Object(), metricNameFormat, excludeFromTTFlags,
                    System.currentTimeMillis());
            txa.addTracer(child);
            child.finish(0, null);
            // child honor the flags and do not make a tt segment
            Assert.assertFalse(child.isTransactionSegment());
        }
        root.finish(0, null);
        Assert.assertEquals("Java/java.lang.Object/dude", TransactionSegment.getMetricName(root));
    }

    @Test
    public void testExcludeChildLeafFromTT() throws Exception {
        setUp(true, true, false);
        Transaction tx = Transaction.getTransaction(true);
        TransactionActivity txa = TransactionActivity.get();
        // make sure the test env is sane
        Assert.assertNotNull(tx);
        Assert.assertNotNull(txa);
        Assert.assertEquals(tx, txa.getTransaction());

        final ClassMethodSignature sig = new ClassMethodSignature("Test", "dude", "()V");
        final MetricNameFormat metricNameFormat = new SimpleMetricNameFormat("Test.dude", "Test.dude");
        final int defaultFlags = TracerFlags.GENERATE_SCOPED_METRIC | TracerFlags.TRANSACTION_TRACER_SEGMENT; // @Trace
        final int leafFlags = defaultFlags | TracerFlags.LEAF;

        DefaultTracer root = new OtherRootTracer(txa, sig, new Object(), metricNameFormat, defaultFlags,
                System.currentTimeMillis());
        txa.tracerStarted(root);
        Assert.assertEquals(txa, root.getTransactionActivity());
        Assert.assertEquals(tx, root.getTransaction());

        final MetricNameFormat mNFChild1 = new SimpleMetricNameFormat("child1", "child1");
        DefaultTracer child1 = new DefaultTracer(txa, sig, new Object(), mNFChild1, leafFlags,
                System.currentTimeMillis());
        txa.tracerStarted(child1);
        //excludeLeaf forces child1 to be marked as excluded
        child1.excludeLeaf();
        child1.finish(0, null);

        final MetricNameFormat mNFChild2 = new SimpleMetricNameFormat("child2", "child2");
        DefaultTracer child2 = new DefaultTracer(txa, sig, new Object(), mNFChild2, leafFlags,
                System.currentTimeMillis());
        txa.tracerStarted(child2);
        child2.finish(0, null);

        root.finish(0, null);

        //TransactionTrace should honor the new excludes flag and drop child1 from the trace
        Map<Tracer, Collection<Tracer>> tt = TransactionTrace.buildChildren(Arrays.asList(root, child1, child2));
        Collection<Tracer> filteredChildren = tt.get(root);
        Assert.assertNotNull(filteredChildren);
        Assert.assertEquals(1, filteredChildren.size());
        Assert.assertTrue(filteredChildren.contains(child2));
    }

    @Test
    public void testTraceWithTimeout() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(null), new HashMap<String, Object>());
        serviceManager.setConfigService(configService);

        AttributesService attService = new AttributesService();
        serviceManager.setAttributesService(attService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        TransactionData td = createTransactionData(TimeoutCause.TOKEN);
        TransactionTrace trace = TransactionTrace.getTransactionTrace(td, SqlObfuscator.getDefaultSqlObfuscator());

        Writer writer = new StringWriter();
        trace.writeJSONString(writer);

        JSONArray serializedTrace = (JSONArray) AgentHelper.serializeJSON(trace);
        JSONArray traceDetails = (JSONArray) decodeTransactionTraceData(serializedTrace.get(4));
        JSONObject atts = (JSONObject) traceDetails.get(4);
        Map<String, Object> intrinsics = (Map<String, Object>) atts.get("intrinsics");
        Assert.assertEquals("token", intrinsics.get(AttributeNames.TIMEOUT_CAUSE));
    }

    @Test
    public void testDistributedTracingAtts() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        Map<String, Object> config = new HashMap<>();
        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put("enabled", true);
        config.put("distributed_tracing", dtConfig);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        config.put("span_events", spanConfig);
        config.put(AgentConfigImpl.APP_NAME, "TransactionTraceTest");
        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(config), config);
        serviceManager.setConfigService(configService);

        serviceManager.setAttributesService(new AttributesService());
        serviceManager.setTransactionTraceService(new TransactionTraceService());
        serviceManager.setTransactionService(new TransactionService());
        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));
        serviceManager.setRPMServiceManager(new MockRPMServiceManager());

        Transaction.clearTransaction();
        Transaction transaction = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("class", "method", "desc");
        OtherRootTracer tracer = new OtherRootTracer(transaction, sig, null, new SimpleMetricNameFormat("metric"));
        transaction.getTransactionActivity().tracerStarted(tracer);
        tracer.finish(Opcodes.ARETURN, null);

        TransactionData td = new TransactionData(transaction, 0);
        ((DistributedTraceServiceImpl) ServiceFactory.getDistributedTraceService()).dispatcherTransactionFinished(td, new TransactionStats());
        TransactionTrace trace = TransactionTrace.getTransactionTrace(td, SqlObfuscator.getDefaultSqlObfuscator());

        Writer writer = new StringWriter();
        trace.writeJSONString(writer);

        JSONArray serializedTrace = (JSONArray) AgentHelper.serializeJSON(trace);
        JSONArray traceDetails = (JSONArray) decodeTransactionTraceData(serializedTrace.get(4));
        JSONObject atts = (JSONObject) traceDetails.get(4);
        Map<String, Object> intrinsics = (Map<String, Object>) atts.get("intrinsics");
        Assert.assertTrue(intrinsics.containsKey("traceId"));
        Assert.assertTrue(intrinsics.containsKey("guid"));
        Assert.assertTrue(intrinsics.containsKey("priority"));
        Assert.assertTrue(intrinsics.containsKey("sampled"));
    }

    public TransactionData createTransactionData(final TimeoutCause cause) {
        return new TransactionData(Mockito.mock(Transaction.class), 2) {
            @Override
            public Map<String, Object> getIntrinsicAttributes() {
                HashMap<String, Object> atts = new HashMap<>();
                atts.put(AttributeNames.TIMEOUT_CAUSE, cause.cause);
                return atts;
            }

            @Override
            public PriorityTransactionName getPriorityTransactionName() {
                return PriorityTransactionName.NONE;
            }

            @Override
            public Dispatcher getDispatcher() {
                return new MockDispatcher();
            }

            @Override
            public Tracer getRootTracer() {
                Tracer mock = Mockito.mock(Tracer.class);
                Mockito.when(mock.getClassMethodSignature()).thenReturn(new ClassMethodSignature("class", "method", "methodDesc"));
                return mock;
            }

            @Override
            public TransactionTimer getTransactionTime() {
                return new TransactionTimer(0);
            }

            @Override
            public String getApplicationName() {
                return "appName";
            }

            @Override
            public TimeoutCause getTimeoutCause() {
                return cause;
            }
        };
    }

    private Object decodeTransactionTraceData(Object object) {
        byte[] bytes = Base64.getDecoder().decode(object.toString());
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        InputStream zipStream = new InflaterInputStream(inputStream);
        Reader in = new InputStreamReader(zipStream);
        return JSONValue.parse(in);
    }
}
