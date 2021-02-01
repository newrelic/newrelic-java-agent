/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Properties;

import static com.newrelic.agent.SaveSystemPropertyProviderRule.TestEnvironmentFacade;
import static com.newrelic.agent.SaveSystemPropertyProviderRule.TestSystemProps;
import static com.newrelic.agent.config.JarCollectorConfigImpl.DEFAULT_ENABLED;
import static com.newrelic.agent.config.JarCollectorConfigImpl.DEFAULT_JARS_PER_SECOND;
import static com.newrelic.agent.config.JarCollectorConfigImpl.DEFAULT_SKIP_TEMP_JARS;
import static com.newrelic.agent.config.JarCollectorConfigImpl.ENABLED;
import static com.newrelic.agent.config.JarCollectorConfigImpl.JARS_PER_SECOND;
import static com.newrelic.agent.config.JarCollectorConfigImpl.SKIP_TEMP_JARS;
import static com.newrelic.agent.config.JarCollectorConfigImpl.SYSTEM_PROPERTY_ROOT;
import static com.newrelic.agent.config.JarCollectorConfigImpl.SYSTEM_PROPERTY_ROOT_DEPRECATED;
import static org.junit.Assert.assertEquals;

public class JarCollectorConfigImplTest {
    private JarCollectorConfig jarCollectorConfig;
    private HashMap<String, Object> configProps;

    // Preferred environment variables
    private static final String NEW_RELIC_JAR_COLLECTOR_ENABLED = "NEW_RELIC_JAR_COLLECTOR_ENABLED";
    private static final String NEW_RELIC_JAR_COLLECTOR_SKIP_TEMP_JARS = "NEW_RELIC_JAR_COLLECTOR_SKIP_TEMP_JARS";
    private static final String NEW_RELIC_JAR_COLLECTOR_JARS_PER_SECOND = "NEW_RELIC_JAR_COLLECTOR_JARS_PER_SECOND";

    // Deprecated environment variables
    private static final String NEW_RELIC_MODULE_ENABLED = "NEW_RELIC_MODULE_ENABLED";

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Before // Resets config props and env vars before each test
    public void before() {
        configProps = new HashMap<>();
    }

    @Test
    public void testDefaultConfigValues() {
        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(DEFAULT_ENABLED, jarCollectorConfig.isEnabled());
        assertEquals(DEFAULT_SKIP_TEMP_JARS, jarCollectorConfig.skipTempJars());
        assertEquals(DEFAULT_JARS_PER_SECOND, jarCollectorConfig.getJarsPerSecond());
    }

    @Test
    public void testLocalConfigValues() {
        // Local config props
        configProps.put(ENABLED, !DEFAULT_ENABLED);
        configProps.put(SKIP_TEMP_JARS, !DEFAULT_SKIP_TEMP_JARS);
        configProps.put(JARS_PER_SECOND, 5);

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(!DEFAULT_ENABLED, jarCollectorConfig.isEnabled());
        assertEquals(!DEFAULT_SKIP_TEMP_JARS, jarCollectorConfig.skipTempJars());
        assertEquals(5, jarCollectorConfig.getJarsPerSecond());
    }

    @Test
    public void testServerConfigValues() {
        // Server config props
        configProps.put(ENABLED, ServerProp.createPropObject(!DEFAULT_ENABLED));
        configProps.put(SKIP_TEMP_JARS, ServerProp.createPropObject(!DEFAULT_SKIP_TEMP_JARS));
        configProps.put(JARS_PER_SECOND, ServerProp.createPropObject(5));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(!DEFAULT_ENABLED, jarCollectorConfig.isEnabled());
        assertEquals(!DEFAULT_SKIP_TEMP_JARS, jarCollectorConfig.skipTempJars());
        assertEquals(5, jarCollectorConfig.getJarsPerSecond());
    }

    @Test
    public void testEnvironmentVariables() {
        TestEnvironmentFacade environmentFacade = new TestEnvironmentFacade(ImmutableMap.of(
                NEW_RELIC_JAR_COLLECTOR_ENABLED, String.valueOf(!DEFAULT_ENABLED),
                NEW_RELIC_JAR_COLLECTOR_SKIP_TEMP_JARS, String.valueOf(!DEFAULT_SKIP_TEMP_JARS),
                NEW_RELIC_JAR_COLLECTOR_JARS_PER_SECOND, "5"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                environmentFacade
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(!DEFAULT_ENABLED, jarCollectorConfig.isEnabled());
        assertEquals(!DEFAULT_SKIP_TEMP_JARS, jarCollectorConfig.skipTempJars());
        assertEquals(5, jarCollectorConfig.getJarsPerSecond());
    }

    @Test
    public void testDeprecatedEnvironmentVariables() {
        TestEnvironmentFacade environmentFacade = new TestEnvironmentFacade(ImmutableMap.of(
                NEW_RELIC_MODULE_ENABLED, String.valueOf(!DEFAULT_ENABLED)
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                environmentFacade
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(!DEFAULT_ENABLED, jarCollectorConfig.isEnabled());
    }

    @Test
    public void testEnvironmentVariablePrecedence() {
        TestEnvironmentFacade environmentFacade = new TestEnvironmentFacade(ImmutableMap.of(
                // Preferred config settings
                NEW_RELIC_JAR_COLLECTOR_ENABLED, String.valueOf(DEFAULT_ENABLED),
                // Deprecated config settings
                NEW_RELIC_MODULE_ENABLED, String.valueOf(!DEFAULT_ENABLED)
        ));

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                environmentFacade
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        // Preferred config settings take precedence over the deprecated settings when both are present
        assertEquals(DEFAULT_ENABLED, jarCollectorConfig.isEnabled());
    }

    @Test
    public void testSystemProperties() {
        Properties props = new Properties();
        props.setProperty(SYSTEM_PROPERTY_ROOT + ENABLED, String.valueOf(!DEFAULT_ENABLED));
        props.setProperty(SYSTEM_PROPERTY_ROOT + SKIP_TEMP_JARS, String.valueOf(!DEFAULT_SKIP_TEMP_JARS));
        props.setProperty(SYSTEM_PROPERTY_ROOT + JARS_PER_SECOND, "5");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(props),
                new TestEnvironmentFacade()
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(!DEFAULT_ENABLED, jarCollectorConfig.isEnabled());
        assertEquals(!DEFAULT_SKIP_TEMP_JARS, jarCollectorConfig.skipTempJars());
        assertEquals(5, jarCollectorConfig.getJarsPerSecond());
    }

    @Test
    public void testDeprecatedSystemProperties() {
        Properties props = new Properties();
        props.setProperty(SYSTEM_PROPERTY_ROOT_DEPRECATED + ENABLED, String.valueOf(!DEFAULT_ENABLED));

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(props),
                new TestEnvironmentFacade()
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(!DEFAULT_ENABLED, jarCollectorConfig.isEnabled());
    }

    @Test
    public void testSystemPropertyPrecedence() {
        Properties props = new Properties();
        // Preferred config settings
        props.setProperty(SYSTEM_PROPERTY_ROOT + ENABLED, String.valueOf(DEFAULT_ENABLED));
        // Deprecated config settings
        props.setProperty(SYSTEM_PROPERTY_ROOT_DEPRECATED + ENABLED, String.valueOf(!DEFAULT_ENABLED));

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(props),
                new TestEnvironmentFacade()
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        // Preferred config settings take precedence over the deprecated settings when both are present
        assertEquals(DEFAULT_ENABLED, jarCollectorConfig.isEnabled());
    }
}
