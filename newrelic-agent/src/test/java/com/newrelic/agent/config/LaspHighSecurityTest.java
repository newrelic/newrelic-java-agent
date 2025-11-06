/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.ForceDisconnectException;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LaspHighSecurityTest {

    private static final String APP_NAME = "Unit Test";

    @Test
    public void laspAndHsmEnabled() throws ConfigurationException {
        Map<String, Object> settings = createSecurityProps();
        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(settings);
        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
        AgentConfig agentConfig = AgentConfigFactory.createAgentConfig(settings, null, null);

        assertTrue(agentConfig.laspEnabled());
        assertTrue(agentConfig.isHighSecurity());
        try {
            ConfigServiceFactory.validateConfig(agentConfig, null);
            fail();
        } catch (ForceDisconnectException ex) {
        }
    }

    private Map<String, Object> createSecurityProps() {
        Map<String, Object> map = new HashMap<>();
        map.put("app_name", APP_NAME);
        map.put("security_policies_token", "AAAAAAAAA=");
        map.put("high_security", true);
        return map;

    }

}
