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

import static com.newrelic.agent.config.JarCollectorConfigImpl.DEFAULT_ENABLED;
import static com.newrelic.agent.config.JarCollectorConfigImpl.DEFAULT_MAX_CLASS_LOADERS;
import static com.newrelic.agent.config.JarCollectorConfigImpl.ENABLED;
import static com.newrelic.agent.config.JarCollectorConfigImpl.MAX_CLASS_LOADERS;
import static com.newrelic.agent.config.JarCollectorConfigImpl.SYSTEM_PROPERTY_ROOT;
import static com.newrelic.agent.config.JarCollectorConfigImpl.SYSTEM_PROPERTY_ROOT_DEPRECATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class JarCollectorConfigImplTest {
    private JarCollectorConfig jarCollectorConfig;
    private HashMap<String, Object> configProps;

    // Preferred environment variables
    private final String NEW_RELIC_JAR_COLLECTOR_ENABLED = "NEW_RELIC_JAR_COLLECTOR_ENABLED";
    private final String NEW_RELIC_JAR_COLLECTOR_MAX_CLASS_LOADERS = "NEW_RELIC_JAR_COLLECTOR_MAX_CLASS_LOADERS";

    // Deprecated environment variables
    private final String NEW_RELIC_MODULE_ENABLED = "NEW_RELIC_MODULE_ENABLED";
    private final String NEW_RELIC_MODULE_MAX_CLASS_LOADERS = "NEW_RELIC_MODULE_MAX_CLASS_LOADERS";

    // Preferred system properties
    private final String enabledSystemProperty = SYSTEM_PROPERTY_ROOT + ENABLED;
    private final String maxClassLoadersSystemProperty = SYSTEM_PROPERTY_ROOT + MAX_CLASS_LOADERS;

    // Deprecated system properties
    private final String enabledSystemPropertyDeprecated = SYSTEM_PROPERTY_ROOT_DEPRECATED + ENABLED;
    private final String maxClassLoadersSystemPropertyDeprecated = SYSTEM_PROPERTY_ROOT_DEPRECATED + MAX_CLASS_LOADERS;

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
        assertEquals(DEFAULT_MAX_CLASS_LOADERS, jarCollectorConfig.getMaxClassLoaders());
    }

    @Test
    public void testLocalConfigValues() {
        // Local config props
        configProps.put(ENABLED, true);
        configProps.put(MAX_CLASS_LOADERS, 42);

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(configProps.get(ENABLED), jarCollectorConfig.isEnabled());
        assertEquals(configProps.get(MAX_CLASS_LOADERS), jarCollectorConfig.getMaxClassLoaders());
    }

    @Test
    public void testServerConfigValues() {
        // Server config props
        ServerProp enabledServerProp = ServerProp.createPropObject(!DEFAULT_ENABLED);
        ServerProp maxClassLoadersServerProp = ServerProp.createPropObject(DEFAULT_MAX_CLASS_LOADERS - 1000);

        configProps.put(ENABLED, enabledServerProp);
        configProps.put(MAX_CLASS_LOADERS, maxClassLoadersServerProp);

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(enabledServerProp.getValue(), jarCollectorConfig.isEnabled());
        assertEquals(maxClassLoadersServerProp.getValue(), jarCollectorConfig.getMaxClassLoaders());
    }

    @Test
    public void testEnvironmentVariables() {
        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                NEW_RELIC_JAR_COLLECTOR_ENABLED, "false",
                NEW_RELIC_JAR_COLLECTOR_MAX_CLASS_LOADERS, "321"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(Boolean.parseBoolean(environmentFacade.getenv(NEW_RELIC_JAR_COLLECTOR_ENABLED)), jarCollectorConfig.isEnabled());
        assertEquals(Integer.parseInt(environmentFacade.getenv(NEW_RELIC_JAR_COLLECTOR_MAX_CLASS_LOADERS)), jarCollectorConfig.getMaxClassLoaders());
    }

    @Test
    public void testDeprecatedEnvironmentVariables() {
        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                NEW_RELIC_MODULE_ENABLED, "false",
                NEW_RELIC_MODULE_MAX_CLASS_LOADERS, "321"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(Boolean.parseBoolean(environmentFacade.getenv(NEW_RELIC_MODULE_ENABLED)), jarCollectorConfig.isEnabled());
        assertEquals(Integer.parseInt(environmentFacade.getenv(NEW_RELIC_MODULE_MAX_CLASS_LOADERS)), jarCollectorConfig.getMaxClassLoaders());
    }

    @Test
    public void testEnvironmentVariablePrecedence() {
        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                // Preferred config settings
                NEW_RELIC_JAR_COLLECTOR_ENABLED, "true",
                NEW_RELIC_JAR_COLLECTOR_MAX_CLASS_LOADERS, "321",
                // Deprecated config settings
                NEW_RELIC_MODULE_ENABLED, "false",
                NEW_RELIC_MODULE_MAX_CLASS_LOADERS, "123"
        ));

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        // Preferred config settings take precedence over the deprecated settings when both are present
        assertEquals(Boolean.parseBoolean(environmentFacade.getenv(NEW_RELIC_JAR_COLLECTOR_ENABLED)), jarCollectorConfig.isEnabled());
        assertEquals(Integer.parseInt(environmentFacade.getenv(NEW_RELIC_JAR_COLLECTOR_MAX_CLASS_LOADERS)), jarCollectorConfig.getMaxClassLoaders());

        // Deprecated settings aren't applied
        assertNotEquals(Boolean.parseBoolean(environmentFacade.getenv(NEW_RELIC_MODULE_ENABLED)), jarCollectorConfig.isEnabled());
        assertNotEquals(Integer.parseInt(environmentFacade.getenv(NEW_RELIC_MODULE_MAX_CLASS_LOADERS)), jarCollectorConfig.getMaxClassLoaders());
    }

    @Test
    public void testSystemProperties() {
        Properties props = new Properties();
        props.setProperty(enabledSystemProperty, "true");
        props.setProperty(maxClassLoadersSystemProperty, "77");

        SaveSystemPropertyProviderRule.TestSystemProps testSystemProps = new SaveSystemPropertyProviderRule.TestSystemProps(props);

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                testSystemProps,
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(Boolean.parseBoolean(testSystemProps.getSystemProperty(enabledSystemProperty)), jarCollectorConfig.isEnabled());
        assertEquals(Integer.parseInt(testSystemProps.getSystemProperty(maxClassLoadersSystemProperty)), jarCollectorConfig.getMaxClassLoaders());
    }

    @Test
    public void testDeprecatedSystemProperties() {
        Properties props = new Properties();
        props.setProperty(enabledSystemPropertyDeprecated, "false");
        props.setProperty(maxClassLoadersSystemPropertyDeprecated, "4999");

        SaveSystemPropertyProviderRule.TestSystemProps testSystemProps = new SaveSystemPropertyProviderRule.TestSystemProps(props);
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                testSystemProps,
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        assertEquals(Boolean.parseBoolean(testSystemProps.getSystemProperty(enabledSystemPropertyDeprecated)), jarCollectorConfig.isEnabled());
        assertEquals(Integer.parseInt(testSystemProps.getSystemProperty(maxClassLoadersSystemPropertyDeprecated)), jarCollectorConfig.getMaxClassLoaders());
    }

    @Test
    public void testSystemPropertyPrecedence() {
        Properties props = new Properties();
        // Preferred config settings
        props.setProperty(enabledSystemProperty, "true");
        props.setProperty(maxClassLoadersSystemProperty, "77");

        // Deprecated config settings
        props.setProperty(enabledSystemPropertyDeprecated, "false");
        props.setProperty(maxClassLoadersSystemPropertyDeprecated, "4999");

        SaveSystemPropertyProviderRule.TestSystemProps testSystemProps = new SaveSystemPropertyProviderRule.TestSystemProps(props);
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                testSystemProps,
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        jarCollectorConfig = new JarCollectorConfigImpl(configProps);

        // Preferred config settings take precedence over the deprecated settings when both are present
        assertEquals(Boolean.parseBoolean(testSystemProps.getSystemProperty(enabledSystemProperty)), jarCollectorConfig.isEnabled());
        assertEquals(Integer.parseInt(testSystemProps.getSystemProperty(maxClassLoadersSystemProperty)), jarCollectorConfig.getMaxClassLoaders());

        // Deprecated settings aren't applied
        assertNotEquals(Boolean.parseBoolean(testSystemProps.getSystemProperty(enabledSystemPropertyDeprecated)), jarCollectorConfig.isEnabled());
        assertNotEquals(Integer.parseInt(testSystemProps.getSystemProperty(maxClassLoadersSystemPropertyDeprecated)), jarCollectorConfig.getMaxClassLoaders());
    }
}
