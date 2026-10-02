/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.newrelic.agent.SaveSystemPropertyProviderRule.TestEnvironmentFacade;
import static com.newrelic.agent.SaveSystemPropertyProviderRule.TestSystemProps;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OtlpExportConfigImplTest {

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    private final Map<String, Object> configProps = new HashMap<>();

    @Before
    public void setup() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(new TestSystemProps(), new TestEnvironmentFacade()));
    }

    @Test
    public void defaultsAreDisabled() {
        OtlpExportConfig config = new OtlpExportConfigImpl(null, "", false);

        assertFalse(config.isEnabled());
        assertFalse(config.isLogsEnabled());
        assertFalse(config.isSpansEnabled());
        assertEquals("https://collector.newrelic.com/v1/logs", config.getLogsEndpoint());
        assertEquals("https://collector.newrelic.com/v1/traces", config.getSpansEndpoint());
        assertTrue(config.getLogsHeaders().isEmpty());
        assertTrue(config.getSpansHeaders().isEmpty());
    }

    @Test
    public void enablingTurnsOnBothSignalsByDefault() {
        configProps.put("enabled", true);

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "", false);

        assertTrue(config.isEnabled());
        assertTrue(config.isLogsEnabled());
        assertTrue(config.isSpansEnabled());
    }

    @Test
    public void signalsCanBeDisabledIndividually() {
        configProps.put("enabled", true);
        configProps.put("logs", Collections.singletonMap("enabled", false));

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "", false);

        assertFalse(config.isLogsEnabled());
        assertTrue(config.isSpansEnabled());
    }

    @Test
    public void serverlessModeDisablesExport() {
        configProps.put("enabled", true);

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "", true);

        assertFalse(config.isEnabled());
        assertFalse(config.isLogsEnabled());
        assertFalse(config.isSpansEnabled());
    }

    @Test
    public void endpointIsDerivedFromRegion() {
        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "eu01", false);

        assertEquals("https://collector.eu01.nr-data.net/v1/logs", config.getLogsEndpoint());
        assertEquals("https://collector.eu01.nr-data.net/v1/traces", config.getSpansEndpoint());
    }

    @Test
    public void configuredEndpointWinsAndTrailingSlashesAreRemoved() {
        configProps.put("endpoint", " http://collector.internal:4318// ");

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "eu01", false);

        assertEquals("http://collector.internal:4318/v1/logs", config.getLogsEndpoint());
        assertEquals("http://collector.internal:4318/v1/traces", config.getSpansEndpoint());
    }

    @Test
    public void blankEndpointUsesDefault() {
        configProps.put("endpoint", "  ");

        assertEquals("https://collector.newrelic.com/v1/logs", new OtlpExportConfigImpl(configProps, "", false).getLogsEndpoint());
    }

    @Test
    public void signalEndpointsAreUsedAsIs() {
        configProps.put("endpoint", "https://collector.newrelic.com");
        configProps.put("logs", Collections.singletonMap("endpoint", " https://logs.example.com:4318/ingest/logs "));
        configProps.put("spans", Collections.singletonMap("endpoint", "http://localhost:4318/v1/traces"));

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "", false);

        assertEquals("https://logs.example.com:4318/ingest/logs", config.getLogsEndpoint());
        assertEquals("http://localhost:4318/v1/traces", config.getSpansEndpoint());
    }

    @Test
    public void signalWithoutEndpointUsesBaseEndpoint() {
        configProps.put("spans", Collections.singletonMap("endpoint", "http://localhost:4318/v1/traces"));
        configProps.put("logs", Collections.singletonMap("endpoint", "  "));

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "eu01", false);

        assertEquals("https://collector.eu01.nr-data.net/v1/logs", config.getLogsEndpoint());
        assertEquals("http://localhost:4318/v1/traces", config.getSpansEndpoint());
    }

    @Test
    public void headersAreParsedAndInvalidEntriesIgnored() {
        configProps.put("headers", "x-tenant=team-a, api-key = abc=123 ,invalid,=novalue");

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "", false);

        assertEquals(ImmutableMap.of("x-tenant", "team-a", "api-key", "abc=123"), config.getLogsHeaders());
        assertEquals(ImmutableMap.of("x-tenant", "team-a", "api-key", "abc=123"), config.getSpansHeaders());
    }

    @Test
    public void signalHeadersReplaceSharedHeaders() {
        configProps.put("headers", "api-key=nr-key,x-shared=1");
        configProps.put("spans", Collections.singletonMap("headers", "authorization=Bearer token"));

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "", false);

        assertEquals(ImmutableMap.of("api-key", "nr-key", "x-shared", "1"), config.getLogsHeaders());
        assertEquals(Collections.singletonMap("authorization", "Bearer token"), config.getSpansHeaders());
    }

    @Test
    public void emptySignalHeadersSendNoSharedHeaders() {
        configProps.put("headers", "api-key=nr-key");
        configProps.put("logs", Collections.singletonMap("headers", ""));

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "", false);

        assertTrue(config.getLogsHeaders().isEmpty());
        assertEquals(Collections.singletonMap("api-key", "nr-key"), config.getSpansHeaders());
    }

    @Test
    public void environmentVariablesOverrideConfig() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(new TestSystemProps(), new TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_OTLP_EXPORT_ENABLED", "true",
                "NEW_RELIC_OTLP_EXPORT_ENDPOINT", "https://collector.jp.nr-data.net",
                "NEW_RELIC_OTLP_EXPORT_SPANS_ENABLED", "false",
                "NEW_RELIC_OTLP_EXPORT_SPANS_ENDPOINT", "http://localhost:4318/v1/traces"))));

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "", false);

        assertTrue(config.isEnabled());
        assertTrue(config.isLogsEnabled());
        assertFalse(config.isSpansEnabled());
        assertEquals("https://collector.jp.nr-data.net/v1/logs", config.getLogsEndpoint());
        assertEquals("http://localhost:4318/v1/traces", config.getSpansEndpoint());
    }

    @Test
    public void systemPropertiesOverrideConfig() {
        Properties props = new Properties();
        props.setProperty("newrelic.config.otlp_export.enabled", "true");
        props.setProperty("newrelic.config.otlp_export.logs.enabled", "false");
        props.setProperty("newrelic.config.otlp_export.headers", "x-tenant=team-a");
        props.setProperty("newrelic.config.otlp_export.logs.endpoint", "https://logs.example.com/v1/logs");
        props.setProperty("newrelic.config.otlp_export.spans.headers", "authorization=Bearer token");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(new TestSystemProps(props), new TestEnvironmentFacade()));

        OtlpExportConfig config = new OtlpExportConfigImpl(configProps, "", false);

        assertTrue(config.isEnabled());
        assertFalse(config.isLogsEnabled());
        assertTrue(config.isSpansEnabled());
        assertEquals(Collections.singletonMap("x-tenant", "team-a"), config.getLogsHeaders());
        assertEquals(Collections.singletonMap("authorization", "Bearer token"), config.getSpansHeaders());
        assertEquals("https://logs.example.com/v1/logs", config.getLogsEndpoint());
    }

    @Test
    public void agentConfigUsesLicenseKeyRegionForEndpoint() {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(AgentConfigImpl.LICENSE_KEY, "eu01xx1234567890123456789012345678901234");
        localSettings.put(AgentConfigImpl.OTLP_EXPORT, Collections.singletonMap("enabled", true));

        OtlpExportConfig config = AgentConfigImpl.createAgentConfig(localSettings).getOtlpExportConfig();

        assertTrue(config.isEnabled());
        assertEquals("https://collector.eu01.nr-data.net/v1/logs", config.getLogsEndpoint());
    }
}
