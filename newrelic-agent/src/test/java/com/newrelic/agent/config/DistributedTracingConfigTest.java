/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.SaveSystemPropertyProviderRule;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DistributedTracingConfigTest {
    private static final String PRIMARY_APP_ID = "AwesomeApp";
    private static MockServiceManager serviceManager;

    @Before
    public void before() throws Exception {
        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        ServiceFactory.getServiceManager().start();
    }

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Test
    public void testEnabledEnvVar() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.singletonMap(
                        DistributedTracingConfig.ENABLED_ENV_KEY, "true"
                ))
        ));

        Map<String, Object> config = ImmutableMap.of(
                AgentConfigImpl.APP_NAME, PRIMARY_APP_ID,
                "distributed_tracing", Collections.singletonMap("enabled", false),
                "span_events", Collections.singletonMap("collect_span_events", true));

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config),
                Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);

        assertTrue(ServiceFactory.getConfigService().getDistributedTracingConfig(PRIMARY_APP_ID).isEnabled());
        assertTrue(ServiceFactory.getConfigService().getDefaultAgentConfig().getSpanEventsConfig().isEnabled());
    }

    @Test
    public void testDisabledEnvVar() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.singletonMap(
                        DistributedTracingConfig.ENABLED_ENV_KEY, "false"
                ))
        ));

        Map<String, Object> config = ImmutableMap.of(
                AgentConfigImpl.APP_NAME, PRIMARY_APP_ID,
                "distributed_tracing", Collections.singletonMap("enabled", true),
                "span_events", Collections.singletonMap("collect_span_events", true));

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config),
                Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);

        assertFalse(ServiceFactory.getConfigService().getDistributedTracingConfig(PRIMARY_APP_ID).isEnabled());
        assertFalse(ServiceFactory.getConfigService().getDefaultAgentConfig().getSpanEventsConfig().isEnabled());
    }

    @Test
    public void testExcludeNewRelicHeader() {
        assertTrue(new DistributedTracingConfig(new HashMap<String, Object>()).isIncludeNewRelicHeader());//default
        HashMap<String, Object> props = new HashMap<>();
        props.put("exclude_newrelic_header", true);
        assertFalse(new DistributedTracingConfig(props).isIncludeNewRelicHeader());
        props.put("exclude_newrelic_header", false);
        assertTrue(new DistributedTracingConfig(props).isIncludeNewRelicHeader());
    }
}
