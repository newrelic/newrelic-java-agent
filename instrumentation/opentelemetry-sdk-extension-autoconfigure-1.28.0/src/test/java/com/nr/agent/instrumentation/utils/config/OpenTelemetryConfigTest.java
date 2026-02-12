/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.config.SystemPropertyProvider;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Properties;

import static com.newrelic.agent.config.SystemPropertyFactory.setSystemPropertyProvider;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.COMMA_SEPARATOR;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_ENABLED;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_ENABLED_DEFAULT;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_LOGS_ENABLED;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_LOGS_ENABLED_DEFAULT;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_METRICS_ENABLED;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_METRICS_ENABLED_DEFAULT;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_METRICS_EXCLUDE;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_METRICS_INCLUDE;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_TRACES_ENABLED;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_TRACES_ENABLED_DEFAULT;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_TRACES_EXCLUDE;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_TRACES_INCLUDE;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static test.config.util.SaveSystemPropertyProviderRule.TestEnvironmentFacade;
import static test.config.util.SaveSystemPropertyProviderRule.TestSystemProps;

public class OpenTelemetryConfigTest {
    // Env vars
    private static final String NEW_RELIC_OPENTELEMETRY_METRICS_EXCLUDE = "NEW_RELIC_OPENTELEMETRY_METRICS_EXCLUDE";
    private static final String NEW_RELIC_OPENTELEMETRY_TRACES_EXCLUDE = "NEW_RELIC_OPENTELEMETRY_TRACES_EXCLUDE";
    // Sys props
    private static final String NEWRELIC_CONFIG_OPENTELEMETRY_METRICS_EXCLUDE = "newrelic.config.opentelemetry.metrics.exclude";
    private static final String NEWRELIC_CONFIG_OPENTELEMETRY_TRACES_EXCLUDE = "newrelic.config.opentelemetry.traces.exclude";

    private Properties props;
    private TestSystemProps testSystemProps;
    private TestEnvironmentFacade environmentFacade;
    private SystemPropertyProvider systemPropertyProvider;

    @Before
    public void setUp() {
        props = new Properties();
        testSystemProps = new TestSystemProps();
        environmentFacade = new TestEnvironmentFacade();
        systemPropertyProvider = SystemPropertyFactory.getSystemPropertyProvider();
    }

    @Test
    public void testDefaultConfigValues() {
        // By default, all Metrics includes/excludes are empty
        assertTrue(OpenTelemetryConfig.getOpenTelemetryMetricsExcludes().isEmpty());
        assertTrue(OpenTelemetryConfig.getOpenTelemetryMetricsIncludes().isEmpty());

        // By default, all Traces includes/excludes are empty
        assertTrue(OpenTelemetryConfig.getOpenTelemetryTracesExcludes().isEmpty());
        assertTrue(OpenTelemetryConfig.getOpenTelemetryTracesIncludes().isEmpty());

        // By default, all opentelemetry signals are disabled
        assertFalse(OpenTelemetryConfig.isOpenTelemetryEnabled());
        assertFalse(OpenTelemetryConfig.isOpenTelemetrySdkAutoConfigureEnabled());
        assertFalse(OpenTelemetryConfig.isOpenTelemetryMetricsEnabled());
        assertFalse(OpenTelemetryConfig.isOpenTelemetryTracesEnabled());
        assertFalse(OpenTelemetryConfig.isOpenTelemetryLogsEnabled());

        OpenTelemetryConfig.resetSdkMetricExporterConfigForTests();
        assertEquals(OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT, OpenTelemetryConfig.getOpenTelemetryMetricsExportInterval());
        assertEquals(OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT, OpenTelemetryConfig.getOpenTelemetryMetricsExportTimeout());
    }

    @Test
    public void testGetOpenTelemetryMetricsExcludesFromYamlProps() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_METRICS_EXCLUDE, "")).thenReturn("foo,bar,baz");
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_METRICS_INCLUDE, "")).thenReturn("");

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            List<String> openTelemetryMetricsExcludes = OpenTelemetryConfig.getOpenTelemetryMetricsExcludes();
            assertEquals(3, openTelemetryMetricsExcludes.size());
            assertTrue(openTelemetryMetricsExcludes.contains("foo"));
            assertTrue(openTelemetryMetricsExcludes.contains("bar"));
            assertTrue(openTelemetryMetricsExcludes.contains("baz"));
        }
    }

    @Test
    public void testGetOpenTelemetryTracesExcludesFromYamlProps() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_EXCLUDE, "")).thenReturn("foo,bar,baz");
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_INCLUDE, "")).thenReturn("");

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            List<String> openTelemetryTracesExcludes = OpenTelemetryConfig.getOpenTelemetryTracesExcludes();
            assertEquals(3, openTelemetryTracesExcludes.size());
            assertTrue(openTelemetryTracesExcludes.contains("foo"));
            assertTrue(openTelemetryTracesExcludes.contains("bar"));
            assertTrue(openTelemetryTracesExcludes.contains("baz"));
        }
    }

    @Test
    public void testGetOpenTelemetryMetricsExcludesFromEnvVars() {
        environmentFacade = new TestEnvironmentFacade(ImmutableMap.of(
                NEW_RELIC_OPENTELEMETRY_METRICS_EXCLUDE, "hello,foo-bar,goodbye"
        ));
        systemPropertyProvider = new SystemPropertyProvider(
                testSystemProps,
                environmentFacade
        );
        setSystemPropertyProvider(systemPropertyProvider);

        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_METRICS_EXCLUDE, "")).thenReturn(environmentFacade.getenv(
                NEW_RELIC_OPENTELEMETRY_METRICS_EXCLUDE));
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_METRICS_INCLUDE, "")).thenReturn("");

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            List<String> openTelemetryMetricsExcludes = OpenTelemetryConfig.getOpenTelemetryMetricsExcludes();
            assertEquals(3, openTelemetryMetricsExcludes.size());
            assertTrue(openTelemetryMetricsExcludes.contains("hello"));
            assertTrue(openTelemetryMetricsExcludes.contains("foo-bar"));
            assertTrue(openTelemetryMetricsExcludes.contains("goodbye"));
        }
    }

    @Test
    public void testGetOpenTelemetryTracesExcludesFromEnvVars() {
        environmentFacade = new TestEnvironmentFacade(ImmutableMap.of(
                NEW_RELIC_OPENTELEMETRY_TRACES_EXCLUDE, "hello,bar-baz,goodbye,foo"
        ));
        systemPropertyProvider = new SystemPropertyProvider(
                testSystemProps,
                environmentFacade
        );
        setSystemPropertyProvider(systemPropertyProvider);

        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_EXCLUDE, "")).thenReturn(environmentFacade.getenv(
                NEW_RELIC_OPENTELEMETRY_TRACES_EXCLUDE));
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_INCLUDE, "")).thenReturn("");

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            List<String> openTelemetryTracesExcludes = OpenTelemetryConfig.getOpenTelemetryTracesExcludes();
            assertEquals(4, openTelemetryTracesExcludes.size());
            assertTrue(openTelemetryTracesExcludes.contains("hello"));
            assertTrue(openTelemetryTracesExcludes.contains("bar-baz"));
            assertTrue(openTelemetryTracesExcludes.contains("goodbye"));
            assertTrue(openTelemetryTracesExcludes.contains("foo"));
        }
    }

    @Test
    public void testExcludeMetersFromSystemProps() {
        props.put(NEWRELIC_CONFIG_OPENTELEMETRY_METRICS_EXCLUDE, "apple,banana");
        testSystemProps = new TestSystemProps(props);
        systemPropertyProvider = new SystemPropertyProvider(
                testSystemProps,
                environmentFacade
        );
        setSystemPropertyProvider(systemPropertyProvider);

        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_METRICS_EXCLUDE, "")).thenReturn(testSystemProps.getSystemProperty(
                NEWRELIC_CONFIG_OPENTELEMETRY_METRICS_EXCLUDE));
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_METRICS_INCLUDE, "")).thenReturn("");

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            List<String> openTelemetryMetricsExcludes = OpenTelemetryConfig.getOpenTelemetryMetricsExcludes();
            assertEquals(2, openTelemetryMetricsExcludes.size());
            assertTrue(openTelemetryMetricsExcludes.contains("apple"));
            assertTrue(openTelemetryMetricsExcludes.contains("banana"));
        }
    }

    @Test
    public void testExcludeTracesFromSystemProps() {
        props.put(NEWRELIC_CONFIG_OPENTELEMETRY_TRACES_EXCLUDE, "apple,banana,boat");
        testSystemProps = new TestSystemProps(props);
        systemPropertyProvider = new SystemPropertyProvider(
                testSystemProps,
                environmentFacade
        );
        setSystemPropertyProvider(systemPropertyProvider);

        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_EXCLUDE, "")).thenReturn(testSystemProps.getSystemProperty(
                NEWRELIC_CONFIG_OPENTELEMETRY_TRACES_EXCLUDE));
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_INCLUDE, "")).thenReturn("");

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            List<String> openTelemetryTracesExcludes = OpenTelemetryConfig.getOpenTelemetryTracesExcludes();
            assertEquals(3, openTelemetryTracesExcludes.size());
            assertTrue(openTelemetryTracesExcludes.contains("apple"));
            assertTrue(openTelemetryTracesExcludes.contains("banana"));
            assertTrue(openTelemetryTracesExcludes.contains("boat"));
        }
    }

    @Test
    public void testIsOpenTelemetryTracerDisabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_EXCLUDE, "")).thenReturn("foo");
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_INCLUDE, "")).thenReturn("");

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            // The 'foo' tracer is disabled because it is in the exclude list
            assertTrue(OpenTelemetryConfig.isOpenTelemetryTracerDisabled("foo"));
            // The 'bar' tracer is enabled
            assertFalse(OpenTelemetryConfig.isOpenTelemetryTracerDisabled("bar"));
        }
    }

    @Test
    public void testGetUniqueStringsFromString() {
        List<String> splitStrings = OpenTelemetryConfig.getUniqueStringsFromString("one,two, three , four, five,six", COMMA_SEPARATOR);
        assertEquals(6, splitStrings.size());
        assertTrue(splitStrings.contains("one"));
        assertTrue(splitStrings.contains("two"));
        assertTrue(splitStrings.contains("three"));
        assertTrue(splitStrings.contains("four"));
        assertTrue(splitStrings.contains("five"));
        assertTrue(splitStrings.contains("six"));
    }

    @Test
    public void testOpenTelemetryEnabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_ENABLED, OPENTELEMETRY_ENABLED_DEFAULT)).thenReturn(true);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            assertTrue(OpenTelemetryConfig.isOpenTelemetryEnabled());
        }
    }

    @Test
    public void testOpenTelemetryEnabledFalse() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_ENABLED, OPENTELEMETRY_ENABLED_DEFAULT)).thenReturn(false);
        // Set the individual logs, metrics, and traces signals as enabled
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_LOGS_ENABLED, OPENTELEMETRY_LOGS_ENABLED_DEFAULT)).thenReturn(true);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_METRICS_ENABLED, OPENTELEMETRY_METRICS_ENABLED_DEFAULT)).thenReturn(true);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_ENABLED, OPENTELEMETRY_TRACES_ENABLED_DEFAULT)).thenReturn(true);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            assertFalse(OpenTelemetryConfig.isOpenTelemetryEnabled());
            // The individual logs, metrics, and traces signals will evaluate as disabled since opentelemetry.enabled is false
            assertFalse(OpenTelemetryConfig.isOpenTelemetryLogsEnabled());
            assertFalse(OpenTelemetryConfig.isOpenTelemetryMetricsEnabled());
            assertFalse(OpenTelemetryConfig.isOpenTelemetryTracesEnabled());
        }
    }

    @Test
    public void testOpenTelemetryLogsEnabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_ENABLED, OPENTELEMETRY_ENABLED_DEFAULT)).thenReturn(true);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_LOGS_ENABLED, OPENTELEMETRY_LOGS_ENABLED_DEFAULT)).thenReturn(true);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            assertTrue(OpenTelemetryConfig.isOpenTelemetryLogsEnabled());
        }
    }

    @Test
    public void testOpenTelemetryMetricsEnabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_ENABLED, OPENTELEMETRY_ENABLED_DEFAULT)).thenReturn(true);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_METRICS_ENABLED, OPENTELEMETRY_METRICS_ENABLED_DEFAULT)).thenReturn(true);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            assertTrue(OpenTelemetryConfig.isOpenTelemetryMetricsEnabled());
        }
    }

    @Test
    public void testOpenTelemetryTracesEnabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_ENABLED, OPENTELEMETRY_ENABLED_DEFAULT)).thenReturn(true);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_ENABLED, OPENTELEMETRY_TRACES_ENABLED_DEFAULT)).thenReturn(true);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            assertTrue(OpenTelemetryConfig.isOpenTelemetryTracesEnabled());
        }
    }

    @Test
    public void testGetOpenTelemetryMetricsExportIntervalValid() {
        int expectedInterval = 10_000; // export_interval needs to be >= to the export_timeout to be valid
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL, OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT))
                .thenReturn(expectedInterval);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            OpenTelemetryConfig.resetSdkMetricExporterConfigForTests();
            // configured export_interval should be respected
            assertEquals(expectedInterval, OpenTelemetryConfig.getOpenTelemetryMetricsExportInterval());
        }
    }

    @Test
    public void testGetOpenTelemetryMetricsExportIntervalInvalid() {
        int expectedInterval = 9_999; // export_interval needs to be >= to the export_timeout to be valid
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL, OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT))
                .thenReturn(expectedInterval);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            OpenTelemetryConfig.resetSdkMetricExporterConfigForTests();
            // reverts to default export_interval
            assertEquals(OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT, OpenTelemetryConfig.getOpenTelemetryMetricsExportInterval());
        }
    }

    @Test
    public void testGetOpenTelemetryMetricsExportIntervalValidExplicit() {
        int expectedInterval = 9_999; // export_interval needs to be >= to the export_timeout to be valid
        int expectedTimeout = 9_999; // explicitly configure export_timeout to a non-default value
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT, OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT))
                .thenReturn(expectedTimeout);

        Mockito.when(mockAgent.getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL, OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT))
                .thenReturn(expectedInterval);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            OpenTelemetryConfig.resetSdkMetricExporterConfigForTests();
            // configured values for both export_timeout and export_interval should be respected
            assertEquals(expectedInterval, OpenTelemetryConfig.getOpenTelemetryMetricsExportInterval());
            assertEquals(expectedTimeout, OpenTelemetryConfig.getOpenTelemetryMetricsExportTimeout());
        }
    }

    @Test
    public void testGetOpenTelemetryMetricsExportIntervalInvalidExplicit() {
        int expectedInterval = 9_998; // export_interval needs to be >= to the export_timeout to be valid
        int expectedTimeout = 9_999; // explicitly configure export_timeout to a non-default value
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT, OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT))
                .thenReturn(expectedTimeout);

        Mockito.when(mockAgent.getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL, OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT))
                .thenReturn(expectedInterval);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            OpenTelemetryConfig.resetSdkMetricExporterConfigForTests();
            // both export_timeout and export_interval should be reset to their defaults
            assertEquals(OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT, OpenTelemetryConfig.getOpenTelemetryMetricsExportInterval());
            assertEquals(OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT, OpenTelemetryConfig.getOpenTelemetryMetricsExportTimeout());
        }
    }

    @Test
    public void testGetOpenTelemetryMetricsExportTimeoutValid() {
        int expectedTimeout = 10_999; // export_timeout needs to be a positive value
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT, OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT))
                .thenReturn(expectedTimeout);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            OpenTelemetryConfig.resetSdkMetricExporterConfigForTests();
            // configured export_timeout should be respected
            assertEquals(expectedTimeout, OpenTelemetryConfig.getOpenTelemetryMetricsExportTimeout());
        }
    }

    @Test
    public void testGetOpenTelemetryMetricsExportTimeoutInvalid() {
        int expectedTimeout = -1; // export_timeout needs to be a positive value
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT, OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT))
                .thenReturn(expectedTimeout);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            OpenTelemetryConfig.resetSdkMetricExporterConfigForTests();
            // reverts to default export_timeout
            assertEquals(OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT, OpenTelemetryConfig.getOpenTelemetryMetricsExportTimeout());
        }
    }
}
