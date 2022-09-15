/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

public class ApplicationLoggingContextDataConfigTest {

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Test
    public void defaultForwardingConfig() {
        Map<String, Object> localProps = new HashMap<>();
        boolean highSecurityDisabled = false;

        ApplicationLoggingContextDataConfig config = new ApplicationLoggingContextDataConfig(localProps, ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT,
                highSecurityDisabled);

        // assert default context data config is created
        assertFalse(config.getEnabled());
        assertTrue(config.getInclude().isEmpty());
        assertTrue(config.getExclude().isEmpty());
    }

    @Test
    public void testEnable() {
        Map<String, Object> localProps = new HashMap<>();
        localProps.put("enabled", Boolean.TRUE);
        localProps.put("include", "asdf,qwer");
        boolean highSecurityDisabled = false;

        ApplicationLoggingContextDataConfig config = new ApplicationLoggingContextDataConfig(localProps, ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT,
                highSecurityDisabled);

        assertTrue(config.getEnabled());
        assertEquals(2, config.getInclude().size());
        assertThat(config.getInclude(), contains("asdf", "qwer"));
        assertTrue(config.getExclude().isEmpty());
    }

    @Test
    public void highSecurityModeOverrides() {
        Map<String, Object> localProps = new HashMap<>();
        localProps.put("enabled", Boolean.TRUE);
        localProps.put("include", "asdf,qwer");
        localProps.put("exclude", "1234,zxcv");
        boolean highSecurityEnabled = true;

        ApplicationLoggingContextDataConfig config = new ApplicationLoggingContextDataConfig(localProps, ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT,
                highSecurityEnabled);

        assertFalse(config.getEnabled());
        assertTrue(config.getInclude().isEmpty());
        assertTrue(config.getExclude().isEmpty());
    }

    @Test
    public void usesEnvVarForNestedConfig() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("NEW_RELIC_APPLICATION_LOGGING_FORWARDING_CONTEXT_DATA_ENABLED", "true");
        envVars.put("NEW_RELIC_APPLICATION_LOGGING_FORWARDING_CONTEXT_DATA_INCLUDE", "include");
        envVars.put("NEW_RELIC_APPLICATION_LOGGING_FORWARDING_CONTEXT_DATA_EXCLUDE", "exclude");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(envVars)
        ));

        ApplicationLoggingContextDataConfig config = new ApplicationLoggingContextDataConfig(Collections.emptyMap(),
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT + "forwarding.", false);

        assertTrue(config.getEnabled());
        assertThat(config.getInclude(), contains("include"));
        assertThat(config.getExclude(), contains("exclude"));
    }

    @Test
    public void usesSysPropForNestedConfig() {
        Properties properties = new Properties();
        properties.put("newrelic.config.application_logging.forwarding.context_data.enabled", "true");
        properties.put("newrelic.config.application_logging.forwarding.context_data.include", "include");
        properties.put("newrelic.config.application_logging.forwarding.context_data.exclude", "exclude");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        ApplicationLoggingContextDataConfig config = new ApplicationLoggingContextDataConfig(Collections.emptyMap(),
                ApplicationLoggingConfigImpl.SYSTEM_PROPERTY_ROOT + "forwarding.", false);

        assertTrue(config.getEnabled());
        assertThat(config.getInclude(), contains("include"));
        assertThat(config.getExclude(), contains("exclude"));
    }

}