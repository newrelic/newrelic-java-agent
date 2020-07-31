/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncludeExcludeCrossAgentTest {

    MockServiceManager manager;

    @Before
    public void setup() {
        manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);
    }

    @Test
    public void testCrossAgentFile() throws Exception {
        List<File> files = AgentHelper.getFiles("com/newrelic/agent/cross_agent_tests/");
        if (files == null || files.isEmpty()) {
            Assert.fail("There were no files read in for testing.");
        } else {
            for (File current : files) {
                if (current.getName().startsWith("attribute")) {
                    processFile(current);
                }
            }
        }
    }

    private void processFile(File current) throws Exception {
        System.out.println("Processing File: " + current);
        JSONArray tests = readJsonAndGetTests(current);
        for (Object currentTest : tests) {
            CrossAgentInput input = new CrossAgentInput((JSONObject) currentTest);
            System.out.println("Running: " + input.getTestName());
            runTest(input);
        }
    }

    private JSONArray readJsonAndGetTests(File file) throws Exception {
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        JSONArray theTests;
        try {
            fr = new FileReader(file);
            theTests = (JSONArray) parser.parse(fr);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                }
            }
        }
        return theTests;
    }

    private void runTest(CrossAgentInput input) {
        ConfigService service = ConfigServiceFactory.createConfigService(input.getAgentConfig(), Collections.<String, Object>emptyMap());
        manager.setConfigService(service);
        AttributesFilter filter = input.createAttributesFilter();
        Map<String, Object> values = createInput(input);
        verifyOutput(filter.filterBrowserAttributes(values), input, AgentConfigImpl.BROWSER_MONITORING);
        verifyOutput(filter.filterErrorEventAttributes(values), input, AgentConfigImpl.ERROR_COLLECTOR);
        verifyOutput(filter.filterTransactionEventAttributes(values), input, AgentConfigImpl.TRANSACTION_EVENTS);
        verifyOutput(filter.filterTransactionTraceAttributes(values), input, AgentConfigImpl.TRANSACTION_TRACER);
    }

    private void verifyOutput(Map<String, ?> actual, CrossAgentInput input, String expected) {
        if (input.getExpectedDestinations().contains(expected)) {
            Assert.assertTrue("Failed Test: " + input.getTestName() + " Key " + input.getInputKey()
                    + " was not found for " + expected, actual.containsKey(input.getInputKey()));
        } else {
            Assert.assertFalse("Failed Test: " + input.getTestName() + " Key " + input.getInputKey()
                    + " was unexpectedly found for " + expected, actual.containsKey(input.getInputKey()));
        }
    }

    private Map<String, Object> createInput(CrossAgentInput input) {
        Map<String, Object> toReturn = new HashMap<>();
        toReturn.put(input.getInputKey(), "hello");
        return toReturn;
    }

}
