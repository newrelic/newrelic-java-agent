/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BaseConfigTest {

    // We are primarily testing that overrides in the environment and the system properties take effect
    // when expected. To do this, we use various hacks to populate the global Environment and
    // SystemProperties collections with our key value, so we need to be able to identify this magic test
    // key so we can set up and clean up using @Before and @After methods.

    private static final String PREFIX = "BaseConfig";
    private static final String KEY = "TestMagicKey";
    private static final String FULL_KEY = PREFIX + KEY;

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();


    @Test(expected = IllegalArgumentException.class)
    public void emptyPrefixNotAllowed() {
        BaseConfig config = new BaseConfig(new HashMap<String, Object>(), "");
        assertNull("Expected exception did not occur", config);
    }

    @Test
    public void getWithSyspropOverride() {
        Map<String, Object> settings = new HashMap<>();
        settings.put(KEY, "valueInMap");
        saveSystemPropertyProviderRule.mockSingleProperty(FULL_KEY, "valueInSystemProperties");

        BaseConfig config = new BaseConfig(settings, PREFIX);
        assertEquals("valueInSystemProperties", config.getProperty(KEY));
        assertNull(config.getProperty(FULL_KEY));

        config = new BaseConfig(settings, "DoesNotMatch");
        assertEquals("valueInMap", config.getProperty(KEY));
        assertNull(config.getProperty(FULL_KEY));

        config = new BaseConfig(settings);
        assertEquals("valueInMap", config.getProperty(KEY));
        assertNull(config.getProperty(FULL_KEY));
    }

    @Test
    public void getWithEnvironmentOverride() {
        Map<String, Object> settings = new HashMap<>();
        settings.put(KEY, "valueInMap");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        KEY, "keyValueInEnvironment",
                        FULL_KEY, "fullKeyValueInEnvironment"
                ))
        ));

        BaseConfig config = new BaseConfig(settings, PREFIX);
        assertEquals("fullKeyValueInEnvironment", config.getProperty(KEY));
        assertNull(config.getProperty(FULL_KEY));

        config = new BaseConfig(settings, "DoesNotMatch");
        assertEquals("valueInMap", config.getProperty(KEY));
        assertNull(config.getProperty(FULL_KEY));

        config = new BaseConfig(settings);
        assertEquals("valueInMap", config.getProperty(KEY));
        assertNull(config.getProperty(FULL_KEY));
    }

    @Test
    public void getWithSyspropAndEnvironmentOverride() {
        Map<String, Object> settings = new HashMap<>();
        settings.put(KEY, "valueInMap");

        Properties props = new Properties();
        props.setProperty(KEY, "keyValueInSystemProperties");
        props.setProperty(FULL_KEY, "fullKeyValueInSystemProperties");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        KEY, "keyValueInEnvironment",
                        FULL_KEY, "fullKeyValueInEnvironment"
                ))
        ));

        BaseConfig config = new BaseConfig(settings, PREFIX);
        assertEquals("fullKeyValueInEnvironment", config.getProperty(KEY));
        assertNull(config.getProperty(FULL_KEY));

        config = new BaseConfig(settings, "DoesNotMatch");
        assertEquals("valueInMap", config.getProperty(KEY));
        assertNull(config.getProperty(FULL_KEY));

        config = new BaseConfig(settings);
        assertEquals("valueInMap", config.getProperty(KEY));
        assertNull(config.getProperty(FULL_KEY));
    }
}
