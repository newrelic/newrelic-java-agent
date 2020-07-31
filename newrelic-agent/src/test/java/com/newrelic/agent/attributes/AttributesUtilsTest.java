/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AttributesUtilsTest {

    @Test
    public void testAppendAttributePrefixesNull() {
        Map<String, Map<String, String>> output = null;
        Assert.assertEquals(0, AttributesUtils.appendAttributePrefixes(output).size());
    }

    @Test
    public void testAppendAttributePrefixesEmpy() {
        Map<String, Map<String, String>> output = new HashMap<>();
        Assert.assertEquals(0, AttributesUtils.appendAttributePrefixes(output).size());
    }

    @Test
    public void testAppendAttributePrefixes() {
        Map<String, Map<String, String>> output = new HashMap<>();
        Assert.assertEquals(0, AttributesUtils.appendAttributePrefixes(output).size());

        Map<String, String> requests = new HashMap<>();
        requests.put("key1", "value1");
        output.put("request.parameters.", requests);
        Map<String, ?> actual = AttributesUtils.appendAttributePrefixes(output);
        Assert.assertEquals(1, actual.size());
        Assert.assertTrue(actual.containsKey("request.parameters.key1"));

        requests.put("key2", "value2");
        requests.put("key3", "value3");
        actual = AttributesUtils.appendAttributePrefixes(output);
        Assert.assertEquals(3, actual.size());
        Assert.assertTrue(actual.containsKey("request.parameters.key1"));
        Assert.assertTrue(actual.containsKey("request.parameters.key2"));
        Assert.assertTrue(actual.containsKey("request.parameters.key3"));
        Assert.assertEquals("value1", actual.get("request.parameters.key1"));
        Assert.assertEquals("value2", actual.get("request.parameters.key2"));
        Assert.assertEquals("value3", actual.get("request.parameters.key3"));

        requests.clear();

        Map<String, String> messages = new HashMap<>();
        messages.put("key1", "message1");
        output.put("message.parameters.", messages);
        requests.put("key1", "value1");
        actual = AttributesUtils.appendAttributePrefixes(output);
        Assert.assertEquals(2, actual.size());
        Assert.assertEquals("value1", actual.get("request.parameters.key1"));
        Assert.assertEquals("message1", actual.get("message.parameters.key1"));

        requests.put("key2", "value2");
        messages.put("key2", "message2");
        messages.put("key3", "message3");
        actual = AttributesUtils.appendAttributePrefixes(output);
        Assert.assertEquals("value1", actual.get("request.parameters.key1"));
        Assert.assertEquals("value2", actual.get("request.parameters.key2"));
        Assert.assertEquals("message1", actual.get("message.parameters.key1"));
        Assert.assertEquals("message2", actual.get("message.parameters.key2"));
        Assert.assertEquals("message3", actual.get("message.parameters.key3"));

    }

}
