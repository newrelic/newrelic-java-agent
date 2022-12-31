/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * LabelsConfigImplTest.java
 */
public class LabelsConfigImplTest {

    @Test
    public void testNull() {
        LabelsConfig config = new LabelsConfigImpl(null);
        assertTrue(config.getLabels().isEmpty());
    }

    @Test
    public void testMap() {
        LabelsConfig config = new LabelsConfigImpl(generateLabels(64));
        assertEquals(64, config.getLabels().size());
        assertEquals(generateLabels(64), config.getLabels());
    }

    @Test
    public void testInvalidKey() {
        Map<String, String> labelsObj = generateLabels(64);
        labelsObj.put("key:38", "value38");
        LabelsConfig config = new LabelsConfigImpl(labelsObj);
        assertEquals(0, config.getLabels().size());

        labelsObj = generateLabels(64);
        labelsObj.put("key;38", "value38");
        config = new LabelsConfigImpl(labelsObj);
        assertEquals(0, config.getLabels().size());

        labelsObj = generateLabels(64);
        labelsObj.put("", "value38");
        config = new LabelsConfigImpl(labelsObj);
        assertEquals(0, config.getLabels().size());
    }

    @Test
    public void testInvalidValue() {
        Map<String, String> labelsObj = generateLabels(64);
        labelsObj.put("key38", "value:38");
        LabelsConfig config = new LabelsConfigImpl(labelsObj);
        assertEquals(0, config.getLabels().size());

        labelsObj = generateLabels(64);
        labelsObj.put("key38", "value;38");
        config = new LabelsConfigImpl(labelsObj);
        assertEquals(0, config.getLabels().size());

        labelsObj = generateLabels(64);
        labelsObj.put("key38", "");
        config = new LabelsConfigImpl(labelsObj);
        assertEquals(0, config.getLabels().size());
    }

    @Test
    public void testTruncateKey() {
        Map<String, String> labelsObj = generateLabels(63);
        labelsObj.put(stringOfLength(1000), "value");
        LabelsConfig config = new LabelsConfigImpl(labelsObj);
        assertEquals(64, config.getLabels().size());
        assertEquals("value", config.getLabels().get(stringOfLength(255)));
    }

    @Test
    public void testTruncateValue() {
        Map<String, String> labelsObj = generateLabels(64);
        labelsObj.put("key38", stringOfLength(1000));
        LabelsConfig config = new LabelsConfigImpl(labelsObj);
        assertEquals(64, config.getLabels().size());
        assertEquals(stringOfLength(255), config.getLabels().get("key38"));
    }

    @Test
    public void testTooManyLabels() {
        LabelsConfig config = new LabelsConfigImpl(generateLabels(1000));
        assertEquals(64, config.getLabels().size());
        assertEquals(generateLabels(64), config.getLabels()); // make sure the first 64 are preserved
    }

    @Test
    public void testNonStringMapValueType() {
        Map<String, Object> labelsObj = new HashMap<>();
        labelsObj.put("key", 1234L);
        LabelsConfig config = new LabelsConfigImpl(labelsObj);
        assertEquals(1, config.getLabels().size());
        assertEquals("1234", config.getLabels().get("key"));
    }

    @Test
    public void testString() {
        String labelsObj = encodeString(generateLabels(64));
        LabelsConfig config = new LabelsConfigImpl(labelsObj);
        assertEquals(64, config.getLabels().size());
        assertEquals(generateLabels(64), config.getLabels());

        config = new LabelsConfigImpl(";;" + labelsObj);
        assertEquals(64, config.getLabels().size());
        assertEquals(generateLabels(64), config.getLabels());

        config = new LabelsConfigImpl(labelsObj + ";;");
        assertEquals(64, config.getLabels().size());
        assertEquals(generateLabels(64), config.getLabels());

        config = new LabelsConfigImpl(";;" + labelsObj + ";;");
        assertEquals(64, config.getLabels().size());
        assertEquals(generateLabels(64), config.getLabels());
    }

    @Test
    public void testInvalidString() {
        LabelsConfig config = new LabelsConfigImpl("too:many:values");
        assertTrue(config.getLabels().isEmpty());

        config = new LabelsConfigImpl("foo:bar;invalid:");
        assertTrue(config.getLabels().isEmpty());

        config = new LabelsConfigImpl("invalid;foo:bar");
        assertTrue(config.getLabels().isEmpty());

        config = new LabelsConfigImpl("bar:foo;invalid;foo:bar");
        assertTrue(config.getLabels().isEmpty());

        config = new LabelsConfigImpl("foo:bar;;many:delimiters");
        assertTrue(config.getLabels().isEmpty());

        config = new LabelsConfigImpl("foo:bar;empty:    ");
        assertTrue(config.getLabels().isEmpty());
    }

    private static String stringOfLength(int length) {
        if (length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(".");
        }
        return sb.toString();
    }

    private static String encodeString(Map<String, String> labels) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            sb.append(entry.getKey());
            sb.append(":");
            sb.append(entry.getValue());
            sb.append(";");
        }
        return sb.toString();
    }

    private static LinkedHashMap<String, String> generateLabels(int size) {
        LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            labels.put("key" + i, "val" + i);
        }
        return labels;
    }
}
