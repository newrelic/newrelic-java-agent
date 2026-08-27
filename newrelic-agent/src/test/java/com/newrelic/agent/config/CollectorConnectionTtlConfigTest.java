/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class CollectorConnectionTtlConfigTest {
    private static final String SYSTEM_PROPERTY = "newrelic.config.collector_connection_ttl";
    private static final String ENVIRONMENT_VARIABLE = "NEW_RELIC_COLLECTOR_CONNECTION_TTL";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "unset uses unlimited default", null, null, null, 0L },
                { "zero disables TTL", 0, null, null, 0L },
                { "positive YAML value is converted to milliseconds", 120, null, null, 120_000L },
                { "system property overrides YAML", 120, "60", null, 60_000L },
                { "environment variable overrides system property", 120, "60", "30", 30_000L },
                { "negative value uses unlimited default", -1, null, null, 0L },
                { "fractional value uses unlimited default", 1.5, null, null, 0L },
                { "non-numeric value uses unlimited default", "invalid", null, null, 0L }
        });
    }

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    private final Object yamlValue;
    private final String systemPropertyValue;
    private final String environmentValue;
    private final long expectedTtlInMillis;

    public CollectorConnectionTtlConfigTest(String testName, Object yamlValue, String systemPropertyValue,
            String environmentValue, long expectedTtlInMillis) {
        this.yamlValue = yamlValue;
        this.systemPropertyValue = systemPropertyValue;
        this.environmentValue = environmentValue;
        this.expectedTtlInMillis = expectedTtlInMillis;
    }

    @Test
    public void resolvesCollectorConnectionTtl() {
        Properties systemProperties = new Properties();
        if (systemPropertyValue != null) {
            systemProperties.setProperty(SYSTEM_PROPERTY, systemPropertyValue);
        }
        Map<String, String> environment = environmentValue == null
                ? Collections.<String, String>emptyMap()
                : Collections.singletonMap(ENVIRONMENT_VARIABLE, environmentValue);
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(systemProperties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(environment)));

        Map<String, Object> localConfig = new HashMap<>();
        if (yamlValue != null) {
            localConfig.put(AgentConfigImpl.COLLECTOR_CONNECTION_TTL, yamlValue);
        }

        AgentConfig config = AgentConfigImpl.createAgentConfig(localConfig);

        assertEquals(expectedTtlInMillis, config.getCollectorConnectionTtlInMilliseconds());
    }
}
