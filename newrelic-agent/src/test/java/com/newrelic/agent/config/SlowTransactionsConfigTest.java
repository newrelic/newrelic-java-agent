/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.newrelic.agent.SaveSystemPropertyProviderRule.TestEnvironmentFacade;
import static com.newrelic.agent.SaveSystemPropertyProviderRule.TestSystemProps;
import static com.newrelic.agent.config.SlowTransactionsConfigImpl.DEFAULT_ENABLED;
import static com.newrelic.agent.config.SlowTransactionsConfigImpl.DEFAULT_THRESHOLD_MILLIS;
import static com.newrelic.agent.config.SlowTransactionsConfigImpl.ENABLED;
import static com.newrelic.agent.config.SlowTransactionsConfigImpl.THRESHOLD;
import static org.junit.Assert.assertEquals;

public class SlowTransactionsConfigTest {

    private final Map<String, Object> configProps = new HashMap<>();
    private SlowTransactionsConfig config;

    @Before
    public void setup() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                new TestEnvironmentFacade()));
    }

    @Test
    public void defaultConfigValues() {
        config = new SlowTransactionsConfigImpl(configProps);

        assertEquals(DEFAULT_ENABLED, config.isEnabled());
        assertEquals(DEFAULT_THRESHOLD_MILLIS, config.getThresholdMillis());
    }

    @Test
    public void configValues() {
        // Local config props
        configProps.put(ENABLED, !DEFAULT_ENABLED);
        configProps.put(THRESHOLD, 5);

        config = new SlowTransactionsConfigImpl(configProps);

        assertEquals(!DEFAULT_ENABLED, config.isEnabled());
        assertEquals(5, config.getThresholdMillis());
    }

    @Test
    public void testEnvironmentVariables() {
        TestEnvironmentFacade environmentFacade = new TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_SLOW_TRANSACTIONS_ENABLED", String.valueOf(!DEFAULT_ENABLED),
                "NEW_RELIC_SLOW_TRANSACTIONS_THRESHOLD", "5"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                environmentFacade
        ));

        config = new SlowTransactionsConfigImpl(configProps);

        assertEquals(!DEFAULT_ENABLED, config.isEnabled());
        assertEquals(5, config.getThresholdMillis());
    }

    @Test
    public void testSystemProperties() {
        Properties props = new Properties();
        props.setProperty("newrelic.config.slow_transactions.enabled", String.valueOf(!DEFAULT_ENABLED));
        props.setProperty("newrelic.config.slow_transactions.threshold", "5");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(props),
                new TestEnvironmentFacade()
        ));

        config = new SlowTransactionsConfigImpl(configProps);

        assertEquals(!DEFAULT_ENABLED, config.isEnabled());
        assertEquals(5, config.getThresholdMillis());
    }
}
