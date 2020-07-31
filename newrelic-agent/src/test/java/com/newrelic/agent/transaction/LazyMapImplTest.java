/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.LazyMapImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class LazyMapImplTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testJSON() throws Exception {
        Map<String, String> params = new LazyMapImpl<>();
        params.put("key1", "value1");
        params.put("key2", "value2");
        Map<String, String> params2 = (Map<String, String>) AgentHelper.serializeJSON(params);
        Assert.assertEquals("value2", params2.get("key2"));
        Assert.assertEquals("value1", params2.get("key1"));
    }

    @Test
    public void testEmpty() throws Exception {
        Map<String, String> params = new LazyMapImpl<>();
        Assert.assertEquals(0, params.size());
        Assert.assertEquals(false, params.containsKey("key1"));
        Assert.assertEquals(false, params.containsValue("value1"));
        Assert.assertEquals(0, params.entrySet().size());
        Assert.assertEquals(0, params.values().size());
        Assert.assertEquals(true, params.isEmpty());
        Assert.assertEquals(0, params.keySet().size());
        Assert.assertNull(params.remove("key1"));
        Assert.assertEquals(0, params.size());
    }

    @Test
    public void testNotEmpty() throws Exception {
        Map<String, String> params = new LazyMapImpl<>();
        params.put("key1", "value1");
        Map<String, String> aMap = new HashMap<>();
        aMap.put("key2", "value2");
        aMap.put("key3", "value3");
        params.putAll(aMap);
        Assert.assertEquals(3, params.size());
        Assert.assertEquals(true, params.containsKey("key1"));
        Assert.assertEquals(true, params.containsValue("value1"));
        Assert.assertEquals(true, params.containsKey("key2"));
        Assert.assertEquals(true, params.containsValue("value2"));
        Assert.assertEquals(true, params.containsKey("key3"));
        Assert.assertEquals(true, params.containsValue("value3"));
        Assert.assertEquals("value1", params.get("key1"));
        Assert.assertEquals("value2", params.get("key2"));
        Assert.assertEquals("value3", params.get("key3"));
        Assert.assertEquals(3, params.entrySet().size());
        Assert.assertEquals(3, params.values().size());
        Assert.assertEquals(false, params.isEmpty());
        Assert.assertEquals(3, params.keySet().size());
        Assert.assertEquals("value1", params.remove("key1"));
        Assert.assertEquals(false, params.containsKey("key1"));
        Assert.assertEquals(2, params.size());
        params.clear();
        Assert.assertEquals(0, params.size());
    }

}
