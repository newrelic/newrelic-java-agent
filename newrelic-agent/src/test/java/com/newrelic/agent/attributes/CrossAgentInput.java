/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CrossAgentInput {

    /**
     * Read a cross-agent-test file containing an array of json tests.
     */
    public static JSONArray readJsonAndGetTests(File file) throws Exception {
        JSONParser parser = new JSONParser();
        try(FileReader fr = new FileReader(file)) {
            return (JSONArray) parser.parse(fr);
        }
    }

    public static JSONArray readJsonAndGetTests(String fileName) throws Exception {
        return readJsonAndGetTests(AgentHelper.getFile(fileName));
    }

    private final String testName;
    private final AgentConfig configuration;
    private final String inputKey;
    private final Set<String> defaultDestinations;
    private final Set<String> expectedDestinations;

    public CrossAgentInput(JSONObject input) {
        testName = getStringValue("testname", input);
        inputKey = getStringValue("input_key", input);
        defaultDestinations = convertToSet((JSONArray) input.get("input_default_destinations"));
        expectedDestinations = convertToSet((JSONArray) input.get("expected_destinations"));
        configuration = createAgentConfig((JSONObject) input.get("config"), defaultDestinations, inputKey);
    }

    public String getTestName() {
        return testName;
    }

    public String getInputKey() {
        return inputKey;
    }

    public AgentConfig getAgentConfig() {
        return configuration;
    }

    public Set<String> getDefaultDestinations() {
        return defaultDestinations;
    }

    public Set<String> getExpectedDestinations() {
        return expectedDestinations;
    }

    private String getStringValue(String key, JSONObject input) {
        String val = (String) input.get(key);
        Assert.assertNotNull(val);
        return val;
    }

    private Set<String> convertToSet(JSONArray data) {
        Assert.assertNotNull(data);
        Set<String> output = new HashSet<>();
        for (Object current : data) {
            output.add((String) current);
        }
        Assert.assertEquals(data.size(), output.size());
        return output;
    }

    private AgentConfig createAgentConfig(JSONObject config, Set<String> defaultDest, String propForDefault) {
        Assert.assertNotNull(config);
        Map<String, Object> settings = new HashMap<>();

        Map<String, Object> atts = new HashMap<>();
        settings.put("attributes", atts);
        Object value = config.get("attributes.enabled");
        if (value != null) {
            atts.put("enabled", value);
        }
        value = config.get("attributes.exclude");
        if (value != null) {
            atts.put("exclude", value);
        }
        value = config.get("attributes.include");
        if (value != null) {
            atts.put("include", value);
        }

        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        Map<String, Object> browserAtts = new HashMap<>();
        browser.put("attributes", browserAtts);
        value = config.get("browser_monitoring.attributes.enabled");
        if (value != null) {
            browserAtts.put("enabled", value);
        }
        value = config.get("browser_monitoring.attributes.exclude");
        if (value != null) {
            browserAtts.put("exclude", value);
        }
        value = config.get("browser_monitoring.attributes.include");
        if (value != null) {
            browserAtts.put("include", value);
        }

        Map<String, Object> txnEvents = new HashMap<>();
        settings.put("transaction_events", txnEvents);
        Map<String, Object> txnEventAtts = new HashMap<>();
        txnEvents.put("attributes", txnEventAtts);
        value = config.get("transaction_events.attributes.enabled");
        if (value != null) {
            txnEventAtts.put("enabled", value);
        }
        value = config.get("transaction_events.attributes.exclude");
        if (value != null) {
            txnEventAtts.put("exclude", value);
        }
        value = config.get("transaction_events.attributes.include");
        if (value != null) {
            txnEventAtts.put("include", value);
        }

        Map<String, Object> txnTracer = new HashMap<>();
        settings.put("transaction_tracer", txnTracer);
        Map<String, Object> txnTracerAtts = new HashMap<>();
        txnTracer.put("attributes", txnTracerAtts);
        value = config.get("transaction_tracer.attributes.enabled");
        if (value != null) {
            txnTracerAtts.put("enabled", value);
        }
        value = config.get("transaction_tracer.attributes.exclude");
        if (value != null) {
            txnTracerAtts.put("exclude", value);
        }
        value = config.get("transaction_tracer.attributes.include");
        if (value != null) {
            txnTracerAtts.put("include", value);
        }

        Map<String, Object> errorEvents = new HashMap<>();
        settings.put("error_collector", errorEvents);
        Map<String, Object> errorEventAtts = new HashMap<>();
        errorEvents.put("attributes", errorEventAtts);
        value = config.get("error_collector.attributes.enabled");
        if (value != null) {
            errorEventAtts.put("enabled", value);
        }
        value = config.get("error_collector.attributes.exclude");
        if (value != null) {
            errorEventAtts.put("exclude", value);
        }
        value = config.get("error_collector.attributes.include");
        if (value != null) {
            errorEventAtts.put("include", value);
        }

        Map<String, Object> spanEvents = new HashMap<>();
        settings.put("span_events", spanEvents);
        Map<String, Object> spanEventAtts = new HashMap<>();
        spanEvents.put("attributes", spanEventAtts);
        value = config.get("span_events.attributes.enabled");
        if (value != null) {
            spanEventAtts.put("enabled", value);
        }
        value = config.get("span_events.attributes.exclude");
        if (value != null) {
            spanEventAtts.put("exclude", value);
        }
        value = config.get("span_events.attributes.include");
        if (value != null) {
            spanEventAtts.put("include", value);
        }

        Map<String, Object> txnSegments = new HashMap<>();
        settings.put("transaction_segments", txnSegments);
        Map<String, Object> txnSegmentAtts = new HashMap<>();
        txnSegments.put("attributes", txnSegmentAtts);
        value = config.get("transaction_segments.attributes.enabled");
        if (value != null) {
            txnSegmentAtts.put("enabled", value);
        }
        value = config.get("transaction_segments.attributes.exclude");
        if (value != null) {
            txnSegmentAtts.put("exclude", value);
        }
        value = config.get("transaction_segments.attributes.include");
        if (value != null) {
            txnSegmentAtts.put("include", value);
        }

        return AgentConfigImpl.createAgentConfig(settings);
    }

    public AttributesFilter createAttributesFilter() {
        String[] browser;
        String[] errors;
        String[] txnTracer;
        String[] txnEvent;
        String[] spanEvent;
        String[] txnSegment;

        if (!defaultDestinations.contains(AgentConfigImpl.BROWSER_MONITORING)) {
            browser = new String[] { inputKey };
        } else {
            browser = new String[0];
        }

        if (!defaultDestinations.contains(AgentConfigImpl.TRANSACTION_EVENTS)) {
            txnEvent = new String[] { inputKey };
        } else {
            txnEvent = new String[0];
        }

        if (!defaultDestinations.contains(AgentConfigImpl.TRANSACTION_TRACER)) {
            txnTracer = new String[] { inputKey };
        } else {
            txnTracer = new String[0];
        }

        if (!defaultDestinations.contains(AgentConfigImpl.ERROR_COLLECTOR)) {
            errors = new String[] { inputKey };
        } else {
            errors = new String[0];
        }

        if (!defaultDestinations.contains(AgentConfigImpl.SPAN_EVENTS)) {
            spanEvent = new String[] { inputKey };
        } else {
            spanEvent = new String[0];
        }

        if (!defaultDestinations.contains(AgentConfigImpl.TRANSACTION_SEGMENTS)) {
            txnSegment = new String[] { inputKey };
        } else {
            txnSegment = new String[0];
        }

        return new AttributesFilter(configuration, browser, errors, txnEvent, txnTracer, spanEvent, txnSegment);
    }
}
