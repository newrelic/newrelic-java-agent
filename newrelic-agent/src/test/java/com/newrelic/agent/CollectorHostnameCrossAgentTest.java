/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.CrossAgentInput;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.config.SystemPropertyProvider;
import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RunWith(Parameterized.class)
public class CollectorHostnameCrossAgentTest {

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Collection<Object[]> getTests() throws Exception {
        JSONArray theTests = CrossAgentInput.readJsonAndGetTests("com/newrelic/agent/cross_agent_tests/collector_hostname.json");

        List<Object[]> parameters = new LinkedList<>();
        for (Object obj : theTests) {
            parameters.add(new Object[] {
                    ((JSONObject) obj).get("name"),
                    obj
            });
        }

        return parameters;
    }

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public JSONObject jsonTest;

    @Before
    public void setUp() {
        originalProvider = SystemPropertyFactory.getSystemPropertyProvider();
    }

    @After
    public void tearDown() {
        SystemPropertyFactory.setSystemPropertyProvider(originalProvider);
    }

    private void setEnv(String key, String val) {
        TEST_ENV_VARS.put(key, val);
        SystemPropertyProvider testProvider = Mocks.createSystemPropertyProvider(new HashMap<String, String>(), TEST_ENV_VARS);
        SystemPropertyFactory.setSystemPropertyProvider(testProvider);
    }

    private void setConfig(String licenseKey, String overrideHost) {
        Map<String, Object> config = new HashMap<>();
        config.put("license_key", licenseKey);
        if (overrideHost != null) {
            config.put("host", overrideHost);
        }
        config.put("app_name", "Unit Test");
        ConfigService configService = new MockConfigService(AgentConfigImpl.createAgentConfig(config));
        MockServiceManager mockServiceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(mockServiceManager);
    }

    @Test
    public void runTest() {
        String testName = (String) jsonTest.get("name");
        String expectedHostname = (String) jsonTest.get("hostname");

        String hostOverride = (String) jsonTest.get("config_override_host");
        String licenseKey = (String) jsonTest.get("config_file_key");

        String envKey = (String) jsonTest.get("env_key");
        String envHost = (String) jsonTest.get("env_override_host");
        if (envKey != null) {
            setEnv("newrelic.config.license_key", envKey);
        }
        if (envHost != null) {
            setEnv("newrelic.config.host", envHost);
        }
        setConfig(licenseKey, hostOverride);
        String actualHostname = ServiceFactory.getConfigService().getDefaultAgentConfig().getHost();

        Assert.assertEquals("cross agent test '" + testName + "' failed.", expectedHostname, actualHostname);
    }

    private final Map<String, String> TEST_ENV_VARS = new HashMap<>();

    private SystemPropertyProvider originalProvider;
}
