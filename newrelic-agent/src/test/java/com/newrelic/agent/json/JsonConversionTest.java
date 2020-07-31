/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.json;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

public class JsonConversionTest {

    @Test
    public void testStrings() {
        String s;

        s = JSONValue.toJSONString("test");

        Assert.assertEquals("\"test\"", s); // without escaping the string is "test"
        Assert.assertEquals(6, s.length());

        s = JSONValue.toJSONString("\\");

        Assert.assertEquals("\"\\\\\"", s); // without escaping the string is "\\"
        Assert.assertEquals(4, s.length());

        s = JSONValue.toJSONString("c:\\a\\b\\");
        Assert.assertEquals("\"c:\\\\a\\\\b\\\\\"", s); // without escaping the string is "c:\\a\\b\\"
        Assert.assertEquals(12, s.length());

        System.out.println("Hi");

    }

    @Test
    public void testUtf8() throws Exception {
        String s = "Andr\u00e9";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        JSONArray.writeJSONString(Arrays.asList(s), writer);
        writer.flush();

        InputStream in = new ByteArrayInputStream(out.toByteArray());
        InputStreamReader is = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(is);
        String responseBody = reader.readLine();

        JSONParser parser = new JSONParser();
        List<?> response = (List<?>) parser.parse(responseBody);
        String result = (String) response.get(0);
        Assert.assertEquals(result, s);
    }

}
