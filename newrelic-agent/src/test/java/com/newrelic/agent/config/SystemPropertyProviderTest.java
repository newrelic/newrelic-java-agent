/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SystemPropertyProviderTest {
    @Test
    public void testSystemPropertyProviderGeneralSystemProps() {
        Properties props = new Properties();
        props.put("newrelic.config.process_host.display_name", "hello");
        props.put("newrelic.config.app_name", "people");
        props.put("newrelic.config.log_file_name", "logfile.log");

        SystemPropertyProvider provider = new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        );

        assertNotNull("Properties can not be null", provider.getNewRelicPropertiesWithoutPrefix().get("process_host.display_name"));
        assertEquals("hello", provider.getNewRelicPropertiesWithoutPrefix().get("process_host.display_name"));
        assertEquals("people", provider.getNewRelicPropertiesWithoutPrefix().get("app_name"));
        assertEquals("logfile.log", provider.getNewRelicPropertiesWithoutPrefix().get("log_file_name"));
    }

    @Test
    public void testSystemPropertyProviderGeneralEnvVars() {
        Map<String, String> envs = new HashMap<>(System.getenv());
        envs.put("NEW_RELIC_LOG", "logfile.log");

        SystemPropertyProvider provider = new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(envs)
        );

        assertNotNull("Properties can not be null", provider.getNewRelicEnvVarsWithoutPrefix());
        assertEquals("logfile.log", provider.getNewRelicEnvVarsWithoutPrefix().get("log_file_name"));
    }

    @Test
    public void testSystemPropertyProviderGeneralEnvProps() {
        //to cover for a case where config properties get passed as environment variables.
        Map<String, String> envs = new HashMap<>(System.getenv());
        envs.put("newrelic.config.process_host.display_name", "hello");
        envs.put("newrelic.config.app_name", "people");
        envs.put("newrelic.config.log_file_name", "logfile.log");

        SystemPropertyProvider provider = new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(envs)
        );

        assertNotNull("Properties can not be null", provider.getNewRelicEnvVarsWithoutPrefix().get("process_host.display_name"));
        assertEquals("hello", provider.getNewRelicEnvVarsWithoutPrefix().get("process_host.display_name"));
        assertEquals("people", provider.getNewRelicEnvVarsWithoutPrefix().get("app_name"));
        assertEquals("logfile.log", provider.getNewRelicEnvVarsWithoutPrefix().get("log_file_name"));
    }

    @Test
    public void testEnvironmentVariable() {
        Map<String, String> envs = new HashMap<>();
        envs.put("NEW_RELIC_ANALYTICS_EVENTS_MAX_SAMPLES_STORED", "12345");
        envs.put("NEW_RELIC_DATASTORE_DATABASE_NAME_REPORTING_ENABLED", "false");
        envs.put("NEW_RELIC_PROCESS_HOST_DISPLAY_NAME", "hello");
        envs.put("NEW_RELIC_APP_NAME", "people");
        envs.put("KUBERNETES_SERVICE_HOST", "10.96.0.1");
        envs.put("newrelic.config.distributed_tracing.enabled", "true");

        SystemPropertyProvider provider = new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(envs)
        );

        assertEquals("12345", provider.getEnvironmentVariable("newrelic.config.analytics_events.max_samples_stored"));
        assertEquals("false", provider.getEnvironmentVariable("newrelic.config.datastore.database_name_reporting.enabled"));
        assertEquals("hello", provider.getEnvironmentVariable("NEW_RELIC_PROCESS_HOST_DISPLAY_NAME"));
        assertEquals("people", provider.getEnvironmentVariable("NEW_RELIC_APP_NAME"));
        assertEquals("10.96.0.1", provider.getEnvironmentVariable("KUBERNETES_SERVICE_HOST"));
        assertEquals("true", provider.getEnvironmentVariable("newrelic.config.distributed_tracing.enabled"));
    }

    @Test
    public void testGetNewRelicSystemProperties() {
        Properties props = new Properties();
        props.put("newrelic.config.process_host.display_name", "hello");
        props.put("newrelic.config.app_name", "people");
        props.put("newrelic.config.log_file_name", "logfile.log");

        SystemPropertyProvider provider = new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        );

        assertNotNull("Properties can not be null", provider.getNewRelicSystemProperties().get("newrelic.config.process_host.display_name"));
        assertEquals("hello", provider.getNewRelicSystemProperties().get("newrelic.config.process_host.display_name"));
        assertEquals("people", provider.getNewRelicSystemProperties().get("newrelic.config.app_name"));
        assertEquals("logfile.log", provider.getNewRelicSystemProperties().get("newrelic.config.log_file_name"));
    }

    @Test
    public void testGetSystemProperty() {
        Properties props = new Properties();
        props.put("newrelic.config.process_host.display_name", "hello");
        props.put("newrelic.config.app_name", "people");
        props.put("newrelic.config.log_file_name", "logfile.log");

        SystemPropertyProvider provider = new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        );

        assertEquals("hello", provider.getSystemProperty("newrelic.config.process_host.display_name"));
        assertEquals("people", provider.getSystemProperty("newrelic.config.app_name"));
        assertEquals("logfile.log", provider.getSystemProperty("newrelic.config.log_file_name"));
    }

}
