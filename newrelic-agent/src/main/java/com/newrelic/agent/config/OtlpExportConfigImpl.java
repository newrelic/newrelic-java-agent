/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class OtlpExportConfigImpl extends BaseConfig implements OtlpExportConfig {

    public static final String ROOT = "otlp_export";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config." + ROOT + ".";
    public static final String ENABLED = "enabled";
    public static final String ENDPOINT = "endpoint";
    public static final String HEADERS = "headers";
    public static final String LOGS = "logs";
    public static final String SPANS = "spans";

    public static final boolean DEFAULT_ENABLED = false;
    public static final boolean DEFAULT_SIGNAL_ENABLED = true;
    public static final String DEFAULT_ENDPOINT = "https://collector.newrelic.com";
    public static final String LOGS_PATH = "/v1/logs";
    public static final String TRACES_PATH = "/v1/traces";

    private final boolean isEnabled;
    private final boolean isLogsEnabled;
    private final boolean isSpansEnabled;
    private final String logsEndpoint;
    private final String spansEndpoint;
    private final Map<String, String> logsHeaders;
    private final Map<String, String> spansHeaders;

    /**
     * @param props            the otlp_export config stanza
     * @param region           the region parsed from the license key (e.g. "eu01"), or empty if there is none
     * @param serverlessMode   true if the agent is running in serverless mode, where OTLP export is not supported
     */
    public OtlpExportConfigImpl(Map<String, Object> props, String region, boolean serverlessMode) {
        super(props, SYSTEM_PROPERTY_ROOT);
        boolean enabled = getProperty(ENABLED, DEFAULT_ENABLED);
        if (enabled && serverlessMode) {
            Agent.LOG.log(Level.WARNING, "OTLP export is not supported in serverless mode and will be disabled."
                    + " Remove otlp_export.enabled to silence this warning.");
            enabled = false;
        }
        isEnabled = enabled;
        SignalConfig logsConfig = new SignalConfig(nestedProps(LOGS), SYSTEM_PROPERTY_ROOT + LOGS + ".");
        SignalConfig spansConfig = new SignalConfig(nestedProps(SPANS), SYSTEM_PROPERTY_ROOT + SPANS + ".");
        isLogsEnabled = isEnabled && logsConfig.isEnabled();
        isSpansEnabled = isEnabled && spansConfig.isEnabled();

        // Like the OpenTelemetry SDK exporters, a signal specific endpoint is used as is, while the base endpoint has the signal path appended
        String baseEndpoint = parseBaseEndpoint(region);
        logsEndpoint = logsConfig.getEndpoint() != null ? logsConfig.getEndpoint() : baseEndpoint + LOGS_PATH;
        spansEndpoint = spansConfig.getEndpoint() != null ? spansConfig.getEndpoint() : baseEndpoint + TRACES_PATH;

        // Also like the OpenTelemetry SDK exporters, signal specific headers replace the shared headers rather than being merged with them,
        // so that headers meant for one endpoint (such as credentials) aren't sent to a different endpoint
        Map<String, String> sharedHeaders = parseHeaders(this, HEADERS);
        logsHeaders = logsConfig.hasHeaders() ? parseHeaders(logsConfig, LOGS + "." + HEADERS) : sharedHeaders;
        spansHeaders = spansConfig.hasHeaders() ? parseHeaders(spansConfig, SPANS + "." + HEADERS) : sharedHeaders;
    }

    /**
     * If the endpoint is set explicitly, always use it. Otherwise, derive the New Relic OTLP endpoint from the license key region.
     */
    private String parseBaseEndpoint(String region) {
        String configuredEndpoint = getStringPropertyOrNull(ENDPOINT);
        String result;
        if (configuredEndpoint != null && !configuredEndpoint.trim().isEmpty()) {
            result = configuredEndpoint.trim();
        } else if (region == null || region.isEmpty()) {
            result = DEFAULT_ENDPOINT;
        } else {
            result = "https://collector." + region + ".nr-data.net";
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * Headers use the same format as OTEL_EXPORTER_OTLP_HEADERS: a comma separated list of key=value pairs.
     *
     * @param source        the config that contains the headers setting
     * @param settingName   the name of the setting relative to otlp_export, used in log messages
     */
    private static Map<String, String> parseHeaders(BaseConfig source, String settingName) {
        List<String> pairs = source.getUniqueStrings(HEADERS, COMMA_SEPARATOR);
        if (pairs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (String pair : pairs) {
            int separatorIndex = pair.indexOf('=');
            if (separatorIndex <= 0) {
                Agent.LOG.log(Level.WARNING, "Ignoring invalid otlp_export.{0} entry \"{1}\". Entries must use the format key=value.", settingName, pair);
                continue;
            }
            result.put(pair.substring(0, separatorIndex).trim(), pair.substring(separatorIndex + 1).trim());
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean isLogsEnabled() {
        return isLogsEnabled;
    }

    @Override
    public boolean isSpansEnabled() {
        return isSpansEnabled;
    }

    @Override
    public String getLogsEndpoint() {
        return logsEndpoint;
    }

    @Override
    public String getSpansEndpoint() {
        return spansEndpoint;
    }

    @Override
    public Map<String, String> getLogsHeaders() {
        return logsHeaders;
    }

    @Override
    public Map<String, String> getSpansHeaders() {
        return spansHeaders;
    }

    private static class SignalConfig extends BaseConfig {
        SignalConfig(Map<String, Object> props, String systemPropertyPrefix) {
            super(props, systemPropertyPrefix);
        }

        boolean isEnabled() {
            return getProperty(ENABLED, DEFAULT_SIGNAL_ENABLED);
        }

        /**
         * @return the configured signal specific endpoint, or null if none is configured
         */
        String getEndpoint() {
            String endpoint = getStringPropertyOrNull(ENDPOINT);
            return endpoint == null || endpoint.trim().isEmpty() ? null : endpoint.trim();
        }

        boolean hasHeaders() {
            return getProperty(HEADERS) != null;
        }
    }
}
