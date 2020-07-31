/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class RequestUriConfigTest {

    final String testName;
    final Map<String, Object> config;
    final JSONObject input;
    final ArrayList<String> expectedUris;

    public RequestUriConfigTest(JSONObject testSpecification) {
        testName = (String) testSpecification.get("test_name");

        Object input = testSpecification.get("input");
        if (input == null) {
            this.input = null;
        } else if (input instanceof JSONObject) {
            this.input = (JSONObject) input;
        } else {
            this.input = null;
        }

        Object config = this.input.get("configuration");
        if (config == null) {
            this.config = null;
        } else if (config instanceof JSONObject) {
            Iterator<Map.Entry<String, Object>> iterator = ((JSONObject) config).entrySet().iterator();
            Map<String, Object> configMap = new HashMap<>();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                configMap.put(entry.getKey(), entry.getValue());
            }
            this.config = configMap;
        } else {
            this.config = null;
        }

        expectedUris = new ArrayList<>();
        JSONObject expectation = (JSONObject) testSpecification.get("expectation");
        if (expectation != null) {
            Set<Map.Entry<String, Object>> expectedResults = expectation.entrySet();
            for (Map.Entry<String, Object> entry : expectedResults) {
                if (entry.getValue() instanceof JSONArray) {
                    expectedUris.addAll((JSONArray) entry.getValue());
                } else {
                    expectedUris.add((String) entry.getValue());
                }
            }
        } else {
            expectedUris.add("");
        }
    }

    public ArrayList<String> getExpectedUriValues() {
        return expectedUris;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public String getTestName() {
        return testName;
    }
}
