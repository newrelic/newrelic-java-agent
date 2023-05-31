/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.newrelic.agent.profile.IProfile;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.profile.ProfilerParameters;
import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.profile.ThreadType;
import com.newrelic.agent.stats.AbstractStats;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsTest;

public class JSONSerializerTest {

    @Test
    public void jsonSerialization() {
        Locale locale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String string = JSONValue.toJSONString(1.5);
            Assert.assertEquals("1.5", string);
        } finally {
            Locale.setDefault(locale);
        }
    }

    @Test
    public void threadType() {
        Assert.assertEquals("request", JSONValue.toJSONString(ThreadType.BasicThreadType.REQUEST));
    }

    @Test
    public void serializeMetricName() throws Exception {
        MetricName name = MetricName.create("test");
        Assert.assertEquals("{\"name\":\"test\"}", AgentHelper.serializeJSON(name).toString());
        name = MetricName.create("test", "scope");
        Assert.assertEquals("{\"scope\":\"scope\",\"name\":\"test\"}", AgentHelper.serializeJSON(name).toString());
    }

    @Test
    public void serializeMetricData() throws Exception {
        MetricName name = MetricName.create("test");
        MetricData data = MetricData.create(name, AbstractStats.EMPTY_STATS);
        Assert.assertEquals("[{\"name\":\"test\"},[0,0,0,0,0,0]]", AgentHelper.serializeJSON(data).toString());
        data = MetricData.create(name, 666, AbstractStats.EMPTY_STATS);
        String res = AgentHelper.serializeJSON(data).toString();
        Assert.assertEquals("[666,[0,0,0,0,0,0]]", AgentHelper.serializeJSON(data).toString());
    }

    @Test
    public void listOfStats() throws Exception {
        List l = buildListOfMetricData(10);

        Object json = JSONArray.toJSONString(l);
        Assert.assertNotNull(json);

        JSONArray deserialized = (JSONArray) AgentHelper.serializeJSON(l);
        validateListOfMetricData(deserialized, 10);
    }

    @Test
    public void serializeFloatWithoutWrapper() {
        float test = 0.00000000000000009f;

        Object serialized = JSONValue.toJSONString(test);
        Assert.assertEquals("9.0E-17", serialized.toString());
    }

    @Test
    public void serializeParams() throws Exception {
        Map params = new HashMap();

        params.put("http_status", 400L);
        Object obj = new Object();
        params.put("obj_to_string", obj.toString());
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(oStream);
        JSONValue.writeJSONString(params, writer);
        writer.close();
        String json = oStream.toString();
        JSONParser parser = new JSONParser();
        JSONObject deserialized = (JSONObject) parser.parse(json);
        Assert.assertEquals(400L, deserialized.get("http_status"));
        Assert.assertEquals(obj.toString(), deserialized.get("obj_to_string"));
    }

    @Test
    public void serializeLargeProfileData() throws Exception {
        ServiceFactory.setServiceManager(new MockServiceManager());

        List<ProfileData> profileData = new ArrayList<>();
        ProfilerParameters parameters = new ProfilerParameters(0L, 0L, 0L, false, false, true, null, null);
        IProfile profile = new com.newrelic.agent.profile.Profile(parameters);
        StackTraceElement[] stackTraceElements = new StackTraceElement[2048];
        for (int i = 0; i < 2048; i++) {
            stackTraceElements[i] = new StackTraceElement("declaringClass", "methodName", "fileName", i);
        }
        profile.addStackTrace(1, true, ThreadType.BasicThreadType.OTHER, stackTraceElements);
        profileData.add(profile);

        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(oStream);
        JSONValue.writeJSONString(profileData, writer);
        writer.close();
        String profileDataJson = oStream.toString();

        Assert.assertNotNull(profileDataJson);
        // Check to make sure the profile data has a reasonable size
        Assert.assertTrue(profileDataJson.getBytes(StandardCharsets.UTF_8).length > 1024);
    }

    @Test
    public void mapOfListOfStats() throws Exception {
        Map m = new HashMap();

        m.put("ten", buildListOfMetricData(10));
        m.put("five", buildListOfMetricData(5));

        JSONObject deserialized = (JSONObject) AgentHelper.serializeJSON(m);
        validateListOfMetricData(deserialized.get("ten"), 10);
        validateListOfMetricData(deserialized.get("five"), 5);
    }

    @Test
    public void metricDataParams() throws Exception {
        List params = new ArrayList();
        params.add(123);

        long beginTime = System.currentTimeMillis() / 1000;
        params.add(beginTime - 60);
        params.add(beginTime);
        params.add(buildListOfMetricData(10));

        JSONArray deserialized = (JSONArray) AgentHelper.serializeJSON(params);
        Assert.assertEquals(deserialized.size(), 4);
        Assert.assertEquals(123l, deserialized.get(0));

        validateListOfMetricData((JSONArray) deserialized.get(3), 10);
    }

    private List buildListOfMetricData(int size) throws Exception {
        List l = new ArrayList();
        for (int i = 0; i < size; i++) {
            String metricName = "Metric " + Integer.toString(i);
            MetricData md = MetricData.create(MetricName.create(metricName), StatsTest.createStats());
            Stats s = (Stats) md.getStats();
            s.recordDataPoint(i);

            Assert.assertEquals(1, s.getCallCount());
            Assert.assertEquals(i, (int) s.getMaxCallTime());
            Assert.assertEquals(i, (int) s.getTotal());
            l.add(md);

            JSONArray jsonObject = (JSONArray) AgentHelper.serializeJSON(s);

            Assert.assertEquals(6, jsonObject.size());
            Assert.assertEquals(1l, jsonObject.get(0));
            if (i > 0) {
                Assert.assertEquals(i, ((Number) jsonObject.get(1)).intValue());
            }
        }

        return l;
    }

    private void validateListOfMetricData(Object arrayObj, int size) throws Exception {
        JSONArray array = (JSONArray) arrayObj;
        Assert.assertEquals(array.size(), size);

        for (int i = 0; i < size; i++) {
            String metricName = "Metric " + Integer.toString(i);
            JSONArray metricData = (JSONArray) array.get(i);

            JSONObject metricSpec = (JSONObject) metricData.get(0);
            Assert.assertEquals(metricSpec.get("name"), metricName);

            JSONArray stats = (JSONArray) metricData.get(1);
            Assert.assertEquals(1l, stats.get(0));
            if (stats.size() > 1) {
                Assert.assertEquals(i, ((Double) stats.get(1)).intValue());
            }
        }
    }
}
