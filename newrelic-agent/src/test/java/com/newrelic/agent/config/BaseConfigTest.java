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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.newrelic.agent.config.AgentConfigImpl.SYSTEM_PROPERTY_ROOT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BaseConfigTest {

    // We are primarily testing that overrides in the environment and the system properties take effect
    // when expected. To do this, we use various hacks to populate the global Environment and
    // SystemProperties collections with our key value, so we need to be able to identify this magic test
    // key so we can set up and clean up using @Before and @After methods.

    private static final String KEY_PROP = "item";
    private static final String KEY_ENV_VAR = "NEW_RELIC_ITEM";
    private static final String KEY_SYS_PROP = "newrelic.config.item";


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
        settings.put(KEY_PROP, "valueInMap");
        saveSystemPropertyProviderRule.mockSingleProperty(KEY_SYS_PROP, "valueInSystemProperties");

        BaseConfig config = new BaseConfig(settings, SYSTEM_PROPERTY_ROOT);
        assertEquals("valueInSystemProperties", config.getProperty(KEY_PROP));
        assertNull(config.getProperty(KEY_SYS_PROP));

        config = new BaseConfig(settings, "DoesNotMatch");
        assertEquals("valueInMap", config.getProperty(KEY_PROP));
        assertNull(config.getProperty(KEY_SYS_PROP));

        config = new BaseConfig(settings);
        assertEquals("valueInMap", config.getProperty(KEY_PROP));
        assertNull(config.getProperty(KEY_SYS_PROP));
    }

    @Test
    public void getWithEnvironmentOverride() {
        Map<String, Object> settings = new HashMap<>();
        settings.put(KEY_PROP, "valueInMap");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                        KEY_ENV_VAR, "fullKeyValueInEnvironment"
                ))
        ));

        BaseConfig config = new BaseConfig(settings, SYSTEM_PROPERTY_ROOT);
        assertEquals("fullKeyValueInEnvironment", config.getProperty(KEY_PROP));
        assertNull(config.getProperty(KEY_SYS_PROP));

        config = new BaseConfig(settings, "DoesNotMatch");
        assertEquals("valueInMap", config.getProperty(KEY_PROP));
        assertNull(config.getProperty(KEY_SYS_PROP));

        config = new BaseConfig(settings);
        assertEquals("valueInMap", config.getProperty(KEY_PROP));
        assertNull(config.getProperty(KEY_SYS_PROP));
    }

    @Test
    public void getWithSyspropAndEnvironmentOverride() {
        Map<String, Object> settings = new HashMap<>();
        settings.put(KEY_PROP, "valueInMap");

        Properties props = new Properties();
        props.setProperty(KEY_SYS_PROP, "fullKeyValueInSystemProperties");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(Collections.singletonMap(
                        KEY_ENV_VAR, "fullKeyValueInEnvironment"
                ))
        ));

        BaseConfig config = new BaseConfig(settings, SYSTEM_PROPERTY_ROOT);
        assertEquals("fullKeyValueInEnvironment", config.getProperty(KEY_PROP));
        assertNull(config.getProperty(KEY_ENV_VAR));

        config = new BaseConfig(settings, "DoesNotMatch");
        assertEquals("valueInMap", config.getProperty(KEY_PROP));
        assertNull(config.getProperty(KEY_SYS_PROP));

        config = new BaseConfig(settings);
        assertEquals("valueInMap", config.getProperty(KEY_PROP));
        assertNull(config.getProperty(KEY_ENV_VAR));
    }
}
