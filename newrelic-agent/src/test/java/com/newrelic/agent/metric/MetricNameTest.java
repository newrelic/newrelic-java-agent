/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metric;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;

public class MetricNameTest {

    @Test
    public void noScope() {
        String name = "Test";
        MetricName metricName = MetricName.create(name);
        Assert.assertEquals(name, metricName.getName());
        Assert.assertEquals("", metricName.getScope());
    }

    @Test
    public void scope() {
        String name = "Test";
        String scope = "Scope";
        MetricName metricName = MetricName.create(name, scope);
        Assert.assertEquals(name, metricName.getName());
        Assert.assertEquals(scope, metricName.getScope());
    }

    @Test
    public void equal() {
        String name = "Test";
        String scope = "Scope";
        MetricName metricName = MetricName.create(name, scope);
        MetricName metricName2 = MetricName.create(name, scope);
        Assert.assertEquals(metricName, metricName2);
    }

    @Test
    public void nameNotEqual() {
        String name = "Test";
        String scope = "Scope";
        String name2 = "Test2";
        MetricName metricName = MetricName.create(name, scope);
        MetricName metricName2 = MetricName.create(name2, scope);
        Assert.assertNotSame(metricName, metricName2);
    }

    @Test
    public void scopeNotEqual() {
        String name = "Test";
        String scope = "Scope";
        String scope2 = "Scope2";
        MetricName metricName = MetricName.create(name, scope);
        MetricName metricName2 = MetricName.create(name, scope2);
        Assert.assertNotSame(metricName, metricName2);
    }

    @Test
    public void hashCodeNameNotEqual() {
        String name = "Test";
        String name2 = "Test2";
        MetricName metricName = MetricName.create(name);
        MetricName metricName2 = MetricName.create(name2);
        Assert.assertNotSame(metricName.hashCode(), metricName2.hashCode());
    }

    @Test
    public void hashCodeScopeNotEqual() {
        String name = "Test";
        String scope = "Scope";
        String scope2 = "Scope2";
        MetricName metricName = MetricName.create(name, scope);
        MetricName metricName2 = MetricName.create(name, scope2);
        Assert.assertNotSame(metricName.hashCode(), metricName2.hashCode());
    }

    @Test
    public void hashCodeEqual() {
        String name = "Test";
        String name2 = "Test";
        MetricName metricName = MetricName.create(name);
        MetricName metricName2 = MetricName.create(name2);
        Assert.assertEquals(metricName.hashCode(), metricName2.hashCode());
    }

    @Test
    public void testJSON() throws Exception {
        String name = "Test";
        String scope = "Scope";
        MetricName metricName = MetricName.create(name, scope);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        JSONArray.writeJSONString(Arrays.asList(metricName), writer);
        writer.flush();

        InputStream in = new ByteArrayInputStream(out.toByteArray());
        InputStreamReader is = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(is);
        String responseBody = reader.readLine();

        JSONParser parser = new JSONParser();
        List<?> response = (List<?>) parser.parse(responseBody);
        JSONObject jsonObj = JSONObject.class.cast(response.get(0));
        MetricName metricName2 = MetricName.parseJSON(jsonObj);
        Assert.assertEquals(metricName, metricName2);
    }

}
