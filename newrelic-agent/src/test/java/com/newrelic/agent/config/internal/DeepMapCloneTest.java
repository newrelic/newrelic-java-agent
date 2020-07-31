/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.internal;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeepMapCloneTest {
    @Test
    public void clonesAllTypesAndNull() {
        List<Object> sourceList = new ArrayList<>();
        sourceList.add(null);
        sourceList.add("hello");
        sourceList.add(65);
        sourceList.add(Collections.singletonMap("mapkey", "mapval"));

        Map<String, Object> source = new HashMap<>();
        source.put("nullval", null);
        source.put("stringval", "hello");
        source.put("primitiveval", 65);
        source.put("listval", sourceList);
        source.put("mapval", Collections.singletonMap("map1", Collections.singletonMap("map2", "very deep")));

        Map<String, Object> target = DeepMapClone.deepCopy(source);
        assertNotSame(source,target);

        assertTrue(target.containsKey("nullval"));
        assertNull(target.get("nullval"));

        assertTrue(target.containsKey("stringval"));
        assertEquals("hello", target.get("stringval"));

        assertTrue(target.containsKey("primitiveval"));
        assertEquals(65, target.get("primitiveval"));

        assertTrue(target.containsKey("listval"));
        assertTrue(target.get("listval") instanceof List);

        List<?> targetList = (List<?>) target.get("listval");
        assertNotSame(sourceList, targetList);
        assertNull(targetList.get(0));
        assertEquals("hello", targetList.get(1));
        assertEquals(65, targetList.get(2));

        assertTrue(targetList.get(3) instanceof Map);
        assertNotSame(sourceList.get(3), targetList.get(3));
        assertEquals("mapval", ((Map<?, ?>)targetList.get(3)).get("mapkey"));

        assertTrue(target.get("mapval") instanceof Map);
        assertNotSame(source.get("mapval"), target.get("mapval"));
        Map<?, ?> targetMapval = (Map<?, ?>) target.get("mapval");
        Map<?, ?> sourceMapval = (Map<?, ?>) source.get("mapval");

        assertTrue(targetMapval.get("map1") instanceof Map);
        assertNotSame(sourceMapval.get("map1"), targetMapval.get("map1"));
        Map<?, ?> targetMap1 = (Map<?, ?>) targetMapval.get("map1");

        assertEquals("very deep", targetMap1.get("map2"));
    }

    @Test
    public void doesNotReturnNull() {
        assertNotNull(DeepMapClone.deepCopy(null));
    }

}