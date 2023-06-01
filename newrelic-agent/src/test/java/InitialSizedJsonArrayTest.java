/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.transport.InitialSizedJsonArray;

public class InitialSizedJsonArrayTest {

    @Test
    public void testFixedSizeJsonArray() throws Exception {
        InitialSizedJsonArray array = new InitialSizedJsonArray(5);
        array.add(1);
        array.add(2);
        array.add(3);
        array.add(4);
        array.add(5);
        Assert.assertEquals(5, array.size());
        List actual = writeToJson(array);
        Assert.assertEquals(5, actual.size());
        for (int i = 1; i < 6; i++) {
            Assert.assertEquals((long) i, actual.get(i - 1));
        }

        array = new InitialSizedJsonArray(1);
        array.add("Tiger");
        Assert.assertEquals(1, array.size());
        actual = writeToJson(array);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals("Tiger", actual.get(0));

        array = new InitialSizedJsonArray(0);
        Assert.assertEquals(0, array.size());
        actual = writeToJson(array);
        Assert.assertEquals(0, actual.size());
    }

    private List writeToJson(InitialSizedJsonArray array) throws IOException, ParseException {
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(oStream);
        JSONValue.writeJSONString(array, writer);
        writer.flush();
        writer.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(oStream.toByteArray());
        InputStreamReader is = new InputStreamReader(bais, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(is);

        JSONParser parser = new JSONParser();
        Object response = (Object) parser.parse(reader);
        Assert.assertTrue(response instanceof List);
        return (List) response;

    }
}
