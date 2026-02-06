/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.config;

import com.newrelic.api.agent.NewRelic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Configuration for the OpenTelemetry hybrid agent functionality.
 * This config controls which, if any, of the opentelemetry logs, metrics, and traces signals will be captured.
 */
public class OpenTelemetryConfig {
    // We don't have any default excludes, but they should be added here if needed in the future
    public static final Set<String> DEFAULT_METER_EXCLUDES = new HashSet<>();
    public static final Set<String> DEFAULT_TRACER_EXCLUDES = new HashSet<>();

    public static final String COMMA_SEPARATOR = ",";

    public static final String OPENTELEMETRY_ENABLED = "opentelemetry.enabled";
    public static final Boolean OPENTELEMETRY_ENABLED_DEFAULT = false;
    public static final String OPENTELEMETRY_LOGS_ENABLED = "opentelemetry.logs.enabled";
    public static final Boolean OPENTELEMETRY_LOGS_ENABLED_DEFAULT = true;
    public static final String OPENTELEMETRY_METRICS_ENABLED = "opentelemetry.metrics.enabled";
    public static final Boolean OPENTELEMETRY_METRICS_ENABLED_DEFAULT = true;
    public static final String OPENTELEMETRY_TRACES_ENABLED = "opentelemetry.traces.enabled";
    public static final Boolean OPENTELEMETRY_TRACES_ENABLED_DEFAULT = true;
    public static final String OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED = "opentelemetry.sdk.autoconfigure.enabled";
    public static final Boolean OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED_DEFAULT = false;

    public static final String OPENTELEMETRY_METRICS_EXCLUDE = "opentelemetry.metrics.exclude";
    public static final String OPENTELEMETRY_METRICS_INCLUDE = "opentelemetry.metrics.include";
    public static final String OPENTELEMETRY_TRACES_EXCLUDE = "opentelemetry.traces.exclude";
    public static final String OPENTELEMETRY_TRACES_INCLUDE = "opentelemetry.traces.include";

    public static final String OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL = "opentelemetry.metrics.export_interval";
    public static final int OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT = 60_000; // export interval in milliseconds
    public static final String OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT = "opentelemetry.metrics.export_timeout";
    public static final int OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT = 10_000; // export timeout in milliseconds
    private static Integer exportTimeout = getOpenTelemetryMetricsExportTimeout();
    private static Integer exportInterval;

    /**
     * Resets the OTLP Metrics exporter export_timeout and export_interval config values.
     * <p>
     * This should only be used in testing to reset
     * the state of these variables for static mocking.
     * <p>
     * Ideally, we wouldn't need to maintain state in a static config class,
     * but there's an unfortunate interdependency between these config values.
     */
    public static void resetSdkMetricExporterConfigForTests() {
        exportTimeout = null;
        exportInterval = null;
        exportTimeout = getOpenTelemetryMetricsExportTimeout();
    }

    /**
     * Legacy setting that was used to enable SDK exporting of OTel metrics.
     * Kept only for backwards compatability and should not be used.
     * <p>
     * Instead, use opentelemetry.enabled and opentelemetry.metrics.enabled.
     *
     * @return boolean true if OTel metrics are enabled, else false
     */
    public static boolean isOpenTelemetrySdkAutoConfigureEnabled() {
        return NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED, OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED_DEFAULT);
    }

    /**
     * Get config for disabling all OpenTelemetry signals or to allow enablement of individual signals.
     *
     * @return boolean, false if all OTel signals are disabled, true if all OTel signals can be enabled.
     */
    public static boolean isOpenTelemetryEnabled() {
        return NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_ENABLED, OPENTELEMETRY_ENABLED_DEFAULT);
    }

    /**
     * Get config for enabling/disabling OpenTelemetry Log signals.
     *
     * @return boolean, false if OTel Log signals are disabled, true if they are enabled.
     */
    public static boolean isOpenTelemetryLogsEnabled() {
        return isOpenTelemetryEnabled() && NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_LOGS_ENABLED, OPENTELEMETRY_LOGS_ENABLED_DEFAULT);
    }

    /**
     * Get config for enabling/disabling OpenTelemetry Metric signals.
     *
     * @return boolean, false if OTel Metric signals are disabled, true if they are enabled.
     */
    public static boolean isOpenTelemetryMetricsEnabled() {
        return isOpenTelemetryEnabled() && NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_METRICS_ENABLED, OPENTELEMETRY_METRICS_ENABLED_DEFAULT);
    }

    /**
     * Get config for enabling/disabling OpenTelemetry Trace signals.
     *
     * @return boolean, false if OTel Trace signals are disabled, true if they are enabled.
     */
    public static boolean isOpenTelemetryTracesEnabled() {
        return isOpenTelemetryEnabled() && NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_TRACES_ENABLED, OPENTELEMETRY_TRACES_ENABLED_DEFAULT);
    }

    /**
     * Creates a combined list of excludes based on the precedence of:
     * user excludes > user includes > default excludes
     *
     * @return a final combined list of excluded OpenTelemetry Meters
     */
    public static List<String> getOpenTelemetryMetricsExcludes() {
        NewRelic.getAgent().getLogger().log(Level.INFO, "Default excluded OpenTelemetry meters: " + DEFAULT_METER_EXCLUDES);

        // User configured includes take precedence over default excludes, so we filter them out.
        getOpenTelemetryMetricsIncludes().forEach(DEFAULT_METER_EXCLUDES::remove);

        // Combine the remaining default excludes with the user configured excludes for the final excludes list.
        String metricsExclude = NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_METRICS_EXCLUDE, "");
        List<String> excludedMeters = getUniqueStringsFromString(metricsExclude, COMMA_SEPARATOR);
        excludedMeters.addAll(DEFAULT_METER_EXCLUDES);
        return excludedMeters;
    }

    /**
     * Get a list of OpenTelemetry Metrics includes.
     *
     * @return list of OpenTelemetry Metrics includes
     */
    public static List<String> getOpenTelemetryMetricsIncludes() {
        String metricsInclude = NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_METRICS_INCLUDE, "");
        return getUniqueStringsFromString(metricsInclude, COMMA_SEPARATOR);
    }

    /**
     * Creates a combined list of excludes based on the precedence of:
     * user excludes > user includes > default excludes
     *
     * @return a final combined list of excluded OpenTelemetry Tracers
     */
    public static List<String> getOpenTelemetryTracesExcludes() {
        NewRelic.getAgent().getLogger().log(Level.INFO, "Default excluded OpenTelemetry tracers: " + DEFAULT_TRACER_EXCLUDES);

        // User configured includes take precedence over default excludes, so we filter them out.
        getOpenTelemetryTracesIncludes().forEach(DEFAULT_TRACER_EXCLUDES::remove);

        // Combine the remaining default excludes with the user configured excludes for the final excludes list.
        String tracesExclude = NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_TRACES_EXCLUDE, "");
        List<String> excludedTracers = getUniqueStringsFromString(tracesExclude, COMMA_SEPARATOR);
        excludedTracers.addAll(DEFAULT_TRACER_EXCLUDES);
        return excludedTracers;
    }

    /**
     * Get a list of OpenTelemetry Traces includes.
     *
     * @return list of OpenTelemetry Traces includes
     */
    public static List<String> getOpenTelemetryTracesIncludes() {
        String tracesInclude = NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_TRACES_INCLUDE, "");
        return getUniqueStringsFromString(tracesInclude, COMMA_SEPARATOR);
    }

    /**
     * Checks if the provided instrumentationScopeName is in the list of excluded Tracers.
     *
     * @param instrumentationScopeName Name of the OpenTelemetry instrumentation
     * @return true if the Tracer is disabled, false if enabled
     */
    public static boolean isOpenTelemetryTracerDisabled(String instrumentationScopeName) {
        List<String> openTelemetryTracesExcludes = getOpenTelemetryTracesExcludes();
        return openTelemetryTracesExcludes.contains(instrumentationScopeName);
    }

    /**
     * Get the OTLP export interval for dimensional metrics.
     *
     * @return int export interval in milliseconds
     */
    public static int getOpenTelemetryMetricsExportInterval() {
        // if the export_interval hasn't been set yet, then get it from config
        if (exportInterval == null) {
            exportInterval = NewRelic.getAgent().getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL, OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT);
        }

        // if the export_interval has been set, then validate it
        if (exportInterval != null && exportInterval >= exportTimeout) {
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.INFO, "OpenTelemetry dimensional metrics export interval is set to the value of " + exportInterval + " milliseconds.");
            return exportInterval;
        }

        // if the configured export_timeout in invalid, then reset both the export_timeout and export_interval to default values (per agent spec)
        exportTimeout = OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT;
        exportInterval = OPEN_TELEMETRY_METRICS_EXPORT_INTERVAL_DEFAULT;
        NewRelic.getAgent()
                .getLogger()
                .log(Level.WARNING,
                        "OpenTelemetry dimensional metrics export interval is invalid. Setting both export_interval and export_timeout to default values.");

        NewRelic.getAgent()
                .getLogger()
                .log(Level.INFO, "OpenTelemetry dimensional metrics export interval is set to the default value of " + exportInterval + " milliseconds.");

        return exportInterval;
    }

    /**
     * Get the OTLP export timeout for sending each dimensional metrics batch.
     *
     * @return int export timeout in milliseconds
     */
    public static int getOpenTelemetryMetricsExportTimeout() {
        // if the export_timeout hasn't been set yet, then get it from config
        if (exportTimeout == null) {
            exportTimeout = NewRelic.getAgent().getConfig().getValue(OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT, OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT);
        }

        // if the export_timeout has been set, then validate it
        if (exportTimeout != null && exportTimeout > 0) {
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.INFO, "OpenTelemetry dimensional metrics export timeout is set to the value of " + exportTimeout + " milliseconds.");
            return exportTimeout;
        }

        // if the configured export_timeout in invalid, then set it to default export_timeout value
        exportTimeout = OPEN_TELEMETRY_METRICS_EXPORT_TIMEOUT_DEFAULT;

        NewRelic.getAgent()
                .getLogger()
                .log(Level.INFO, "OpenTelemetry dimensional metrics export timeout is set to the default value of " + exportTimeout + " milliseconds.");

        return exportTimeout;
    }

    /**
     * Splits the given values String into a collection of Strings based on the provided separator character.
     *
     * @param valuesString A separator delimited string
     * @param separator    A character delimiter in a string
     * @return List of string split by the separator delimiter
     */
    public static List<String> getUniqueStringsFromString(String valuesString, String separator) {
        List<String> result = new ArrayList<>();
        if (valuesString == null || valuesString.isEmpty()) {
            return result;
        }
        String[] valuesArray = valuesString.split(separator);
        for (String value : valuesArray) {
            value = value.trim();
            if (!value.isEmpty() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }
}
