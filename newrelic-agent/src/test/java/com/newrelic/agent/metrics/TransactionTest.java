/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.attributes.CrossAgentInput;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.test.marker.RequiresFork;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@RunWith(Parameterized.class)
@Category(RequiresFork.class)
public class TransactionTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws Exception {
        List<File> files = AgentHelper.getFiles("com/newrelic/agent/metrics/testcases/");
        List<Object[]> tests = new LinkedList<>();
        for (File file : files) {
            if (file.getAbsolutePath().endsWith("json")) {
                String path = file.getName();
                JSONArray inputTests = CrossAgentInput.readJsonAndGetTests(file);
                for (Object current : inputTests) {
                    JSONObject oneTest = (JSONObject) current;
                    final OneTestForCriticalPath test = OneTestForCriticalPath.createOneTestForCriticalPath(oneTest, path);
                    tests.add(new Object[] { test.getTestName(), test });
                }
            }
        }

        return tests;
    }

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public OneTestForCriticalPath test;

    @Test(timeout = 5000)
    public void runTest() throws Exception {
        test.runTest();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }
}
