/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.LazyMapImpl;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.model.*;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TransactionEventTest {

    private static final String APP_NAME = "testing";
    private static final String metricName = "WebTransaction/Servlet/TestServlet";

    long startTime;

    @Before
    public void setup() {
        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);

        Map<String, Object> map = createConfigMap();
        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map), map);
        manager.setConfigService(configService);
        manager.setStatsService(Mockito.mock(StatsService.class));

        AttributesService attService = new AttributesService();
        manager.setAttributesService(attService);
        startTime = System.currentTimeMillis();
    }

    private Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "name");
        map.put("apdex_t", 0.5f);
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        return map;
    }

    @Test
    public void testJSON() throws Exception {
        float duration = 0.001931f;
        AnalyticsEvent event = baseBuilder(duration)
                .build();
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        assertEquals(1, jsonArray.size());

        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(9, jsonObject.size());
        assertEquals(startTime, jsonObject.get("timestamp"));
        assertEquals(metricName, jsonObject.get("name"));
        assertEquals(duration, ((Number) jsonObject.get("duration")).floatValue(), Float.NaN);
        assertEquals("Transaction", jsonObject.get("type"));
        assertEquals(duration, ((Number) jsonObject.get("totalTime")).floatValue(), Float.NaN);
        assertEquals(.25, ((Number) jsonObject.get("priority")).floatValue(), Float.NaN);
    }

    @Test
    public void testJSONDTIntrinsics() throws Exception {
        float duration = 0.001931f;
        AnalyticsEvent event = baseBuilder(duration)
                .setDistributedTraceIntrinsics(Collections.<String, Object>singletonMap("dt-intrinsic", "yup"))
                .build();
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        assertEquals(1, jsonArray.size());

        JSONObject intrinsicsJson = (JSONObject) jsonArray.get(0);
        assertEquals("yup", intrinsicsJson.get("dt-intrinsic"));
    }

    @Test
    public void testJSONWithTTFLBs() throws Exception {
        float duration = 0.001931f;
        TransactionEvent event = baseBuilder(duration)
                .setTimeToFirstByte(.00182f)
                .setTimeToLastByte(.00211f)
                .build();

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        assertEquals(1, jsonArray.size());

        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(11, jsonObject.size());
        assertEquals(startTime, jsonObject.get("timestamp"));
        assertEquals(metricName, jsonObject.get("name"));
        assertEquals(duration, ((Number) jsonObject.get("duration")).floatValue(), Float.NaN);
        assertEquals("Transaction", jsonObject.get("type"));
        assertEquals(duration, ((Number) jsonObject.get("totalTime")).floatValue(), Float.NaN);
        assertEquals(.00182f, ((Number) jsonObject.get("timeToFirstByte")).floatValue(), Float.NaN);
        assertEquals(.00211f, ((Number) jsonObject.get("timeToLastByte")).floatValue(), Float.NaN);
        assertEquals(true, jsonObject.get("error"));
        assertEquals(.25, ((Number) jsonObject.get("priority")).floatValue(), Float.NaN);
    }

    @Test
    public void testJSONuserParameters() throws Exception {
        float duration = 0.001931f;
        float totalTime = .1345f;
        TransactionEvent event = new TransactionEventBuilder().setAppName(APP_NAME)
                .setTimestamp(startTime)
                .setName(metricName)
                .setDuration(duration)
                .setGuid(null)
                .setReferringGuid(null)
                .setPort(8081)
                .setTripId(null)
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(totalTime)
                .setTimeoutCause(null)
                .setPriority(.25F)
                .putAllUserAttributes(ImmutableMap.<String, Object>of("key1", "value1", "key2", "value2"))
                .build();

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        assertEquals(2, jsonArray.size());

        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(9, jsonObject.size());
        assertEquals(startTime, jsonObject.get("timestamp"));
        assertEquals(metricName, jsonObject.get("name"));
        assertEquals(duration, ((Number) jsonObject.get("duration")).floatValue(), Float.NaN);
        assertEquals("Transaction", jsonObject.get("type"));
        assertEquals(false, jsonObject.get("error"));
        assertEquals(totalTime, ((Number) jsonObject.get("totalTime")).floatValue(), Float.NaN);

        JSONObject jsonObject2 = (JSONObject) jsonArray.get(1);
        assertEquals(2, jsonObject2.size());
        assertEquals("value1", jsonObject2.get("key1"));
        assertEquals("value2", jsonObject2.get("key2"));
        assertEquals(.25, ((Number) jsonObject.get("priority")).floatValue(), Float.NaN);
    }

    @Test
    public void testJSONuserParametersEmpty() throws Exception {
        float duration = 0.001931f;
        TransactionEvent event = new TransactionEventBuilder().setAppName(APP_NAME)
                .setTimestamp(startTime)
                .setName(metricName)
                .setDuration(duration)
                .setGuid(null)
                .setReferringGuid(null)
                .setPort(8081)
                .setTripId(null)
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(duration)
                .setTimeoutCause(null)
                .setPriority(.25F)
                .build();

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        assertEquals(1, jsonArray.size());

        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(9, jsonObject.size());
        assertEquals(startTime, jsonObject.get("timestamp"));
        assertEquals(metricName, jsonObject.get("name"));
        assertEquals(duration, ((Number) jsonObject.get("duration")).floatValue(), Float.NaN);
        assertEquals("Transaction", jsonObject.get("type"));
        assertEquals(false, jsonObject.get("error"));
        assertEquals(duration, ((Number) jsonObject.get("totalTime")).floatValue(), Float.NaN);
        assertEquals(.25, ((Number) jsonObject.get("priority")).floatValue(), Float.NaN);
    }

    @Test
    public void testJSONuserParametersEmptyWithAgentAtts() throws Exception {
        float duration = 0.001931f;
        float totalTime = .012345f;
        TransactionEvent event = new TransactionEventBuilder().setAppName(APP_NAME)
                .setTimestamp(startTime)
                .setName(metricName)
                .setDuration(duration)
                .setGuid(null)
                .setReferringGuid(null)
                .setPort(8081)
                .setTripId(null)
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(totalTime)
                .setTimeoutCause(null)
                .setPriority(.25F)
                .build();

        Map<String, Object> agentAtts = new LazyMapImpl<>();
        agentAtts.put("key1", "value1");
        agentAtts.put("key2", "value2");
        event.agentAttributes = agentAtts;

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        assertEquals(3, jsonArray.size());

        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(9, jsonObject.size());
        assertEquals(startTime, jsonObject.get("timestamp"));
        assertEquals(metricName, jsonObject.get("name"));
        assertEquals(duration, ((Number) jsonObject.get("duration")).floatValue(), Float.NaN);
        assertEquals("Transaction", jsonObject.get("type"));
        assertEquals(false, jsonObject.get("error"));
        assertEquals(totalTime, ((Number) jsonObject.get("totalTime")).floatValue(), Float.NaN);
        assertEquals(.25, ((Number) jsonObject.get("priority")).floatValue(), Float.NaN);

        jsonObject = (JSONObject) jsonArray.get(1);
        assertEquals(0, jsonObject.size());

        jsonObject = (JSONObject) jsonArray.get(2);
        assertEquals(2, jsonObject.size());
        assertEquals("value1", jsonObject.get("key1"));
        assertEquals("value2", jsonObject.get("key2"));
    }

    @Test
    public void testJSONUserAndAgentAttributes() throws Exception {
        float duration = 0.001931f;
        float totalTime = .002934f;
        TransactionEvent event = new TransactionEventBuilder().setAppName(APP_NAME)
                .setTimestamp(startTime)
                .setName(metricName)
                .setDuration(duration)
                .setGuid(null)
                .setReferringGuid(null)
                .setPort(8081)
                .setTripId(null)
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(totalTime)
                .setTimeoutCause(null)
                .setPriority(.25F)
                .putAllUserAttributes(ImmutableMap.<String, Object>of("key1", "value1", "key2", "value2", "user", "value3"))
                .build();

        Map<String, Object> agentAtts = new LazyMapImpl<>();
        agentAtts.put("key1", "value1");
        agentAtts.put("key2", "value2");
        agentAtts.put("agent", "value4");
        event.agentAttributes = agentAtts;

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        assertEquals(3, jsonArray.size());

        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(9, jsonObject.size());
        assertEquals(startTime, jsonObject.get("timestamp"));
        assertEquals(metricName, jsonObject.get("name"));
        assertEquals(duration, ((Number) jsonObject.get("duration")).floatValue(), Float.NaN);
        assertEquals("Transaction", jsonObject.get("type"));
        assertEquals(false, jsonObject.get("error"));
        assertEquals(totalTime, ((Number) jsonObject.get("totalTime")).floatValue(), Float.NaN);
        assertEquals(.25, ((Number) jsonObject.get("priority")).floatValue(), Float.NaN);

        jsonObject = (JSONObject) jsonArray.get(1);
        assertEquals(3, jsonObject.size());
        assertEquals("value1", jsonObject.get("key1"));
        assertEquals("value2", jsonObject.get("key2"));
        assertEquals("value3", jsonObject.get("user"));

        jsonObject = (JSONObject) jsonArray.get(2);
        assertEquals(3, jsonObject.size());
        assertEquals("value1", jsonObject.get("key1"));
        assertEquals("value2", jsonObject.get("key2"));
        assertEquals("value4", jsonObject.get("agent"));
    }

    @Test
    public void testWebRequestAgentAttributes() throws Exception {
        float duration = 0.001931f;
        float totalTime = .002934f;
        TransactionEvent event = new TransactionEventBuilder().setAppName(APP_NAME)
                .setTimestamp(startTime)
                .setName(metricName)
                .setDuration(duration)
                .setGuid(null)
                .setReferringGuid(null)
                .setPort(8081)
                .setTripId(null)
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(totalTime)
                .setTimeoutCause(null)
                .setPriority(.25F)
                .build();

        Map<String, Object> agentAtts = new LazyMapImpl<>();
        agentAtts.put(AttributeNames.REQUEST_ACCEPT_PARAMETER_NAME, "requestAccept");
        agentAtts.put(AttributeNames.REQUEST_CONTENT_LENGTH_PARAMETER_NAME, "requestContentLength");
        agentAtts.put(AttributeNames.REQUEST_HOST_PARAMETER_NAME, "requestHost");
        agentAtts.put(AttributeNames.REQUEST_USER_AGENT_PARAMETER_NAME, "requestUserAgent");
        agentAtts.put(AttributeNames.REQUEST_METHOD_PARAMETER_NAME, "requestMethod");
        agentAtts.put(AttributeNames.RESPONSE_CONTENT_TYPE_PARAMETER_NAME, "responseContentType");
        event.agentAttributes = agentAtts;

        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        assertEquals(3, jsonArray.size());

        // Agent attributes
        JSONObject jsonObject = (JSONObject) jsonArray.get(2);
        assertEquals(6, jsonObject.size());
        assertEquals("requestAccept", jsonObject.get(AttributeNames.REQUEST_ACCEPT_PARAMETER_NAME));
        assertEquals("requestContentLength", jsonObject.get(AttributeNames.REQUEST_CONTENT_LENGTH_PARAMETER_NAME));
        assertEquals("requestHost", jsonObject.get(AttributeNames.REQUEST_HOST_PARAMETER_NAME));
        assertEquals("requestUserAgent", jsonObject.get(AttributeNames.REQUEST_USER_AGENT_PARAMETER_NAME));
        assertEquals("requestMethod", jsonObject.get(AttributeNames.REQUEST_METHOD_PARAMETER_NAME));
        assertEquals("responseContentType", jsonObject.get(AttributeNames.RESPONSE_CONTENT_TYPE_PARAMETER_NAME));
    }

    @Test
    public void testTimeoutAttribute() throws Exception {
        TransactionEvent event = new TransactionEventBuilder().setAppName(APP_NAME)
                .setTimestamp(startTime)
                .setName(metricName)
                .setDuration(0.001931f)
                .setGuid(null).setReferringGuid(null)
                .setPort(8081)
                .setTripId(null)
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(.002934f)
                .setTimeoutCause(TimeoutCause.SEGMENT)
                .setPriority(.25F)
                .build();
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        // Intrinsic attributes
        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals("segment", jsonObject.get("nr.timeoutCause"));

        event = new TransactionEventBuilder()
                .setAppName(APP_NAME)
                .setTimestamp(startTime)
                .setName(metricName)
                .setDuration(0.001931f)
                .setGuid(null)
                .setReferringGuid(null)
                .setPort(8081)
                .setTripId(null)
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(.002934f)
                .setTimeoutCause(TimeoutCause.TOKEN).build();

        jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals("token", jsonObject.get("nr.timeoutCause"));

        event = new TransactionEventBuilder().setAppName(APP_NAME)
                .setTimestamp(startTime)
                .setName(metricName)
                .setDuration(0.001931f)
                .setGuid(null)
                .setReferringGuid(null)
                .setPort(8081)
                .setTripId(null)
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(.002934f)
                .setTimeoutCause(null)
                .setPriority(.25F)
                .build();
        jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        jsonObject = (JSONObject) jsonArray.get(0);
        assertNull(jsonObject.get("nr.timeoutCause"));
    }

    @Test
    public void testJsonWithPathHashes() throws Exception {
        float duration = 0.001931f;
        TransactionEvent event = baseBuilder(duration)
                .setPathHashes(new PathHashes(12, 13, "14"))
                .build();
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(9, jsonObject.size());
        // These should only be set when using CAT, not DT as the agent does by default as of 7.3.0
        assertNull(jsonObject.get("nr.pathHash"));
        assertNull(jsonObject.get("nr.referringPathHash"));
        assertNull(jsonObject.get("nr.alternatePathHashes"));
    }

    @Test
    public void testJsonWithSyntheticIds() throws Exception {
        float duration = 0.001931f;
        TransactionEvent event = baseBuilder(duration)
                .setSyntheticsIds(new SyntheticsIds("101", "102", "103"))
                .build();
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(12, jsonObject.size());
        assertEquals("101", jsonObject.get("nr.syntheticsResourceId"));
        assertEquals("102", jsonObject.get("nr.syntheticsMonitorId"));
        assertEquals("103", jsonObject.get("nr.syntheticsJobId"));
    }

    @Test
    public void testJsonWithSyntheticsInfo() throws Exception {
        float duration = 0.001931f;
        Map<String, String> mapAttrs = new HashMap<>();
        mapAttrs.put("key1", "val1");
        TransactionEvent event = baseBuilder(duration)
                .setSyntheticsInfo(new SyntheticsInfo("cli", "scheduled", mapAttrs))
                .build();
        JSONArray jsonArray = (JSONArray) AgentHelper.serializeJSON(event);
        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
        assertEquals(12, jsonObject.size());
        assertEquals("cli", jsonObject.get("nr.syntheticsInitiator"));
        assertEquals("scheduled", jsonObject.get("nr.syntheticsType"));
        assertEquals("val1", jsonObject.get("nr.syntheticsKey1"));
    }

    private TransactionEventBuilder baseBuilder(float duration) {
        return new TransactionEventBuilder().setAppName(APP_NAME)
                .setTimestamp(startTime)
                .setName(metricName)
                .setDuration(duration)
                .setGuid(null)
                .setReferringGuid(null)
                .setPort(8081)
                .setTripId(null)
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(true)
                .setpTotalTime(duration)
                .setTimeoutCause(null)
                .setPriority(.25F);
    }


}
