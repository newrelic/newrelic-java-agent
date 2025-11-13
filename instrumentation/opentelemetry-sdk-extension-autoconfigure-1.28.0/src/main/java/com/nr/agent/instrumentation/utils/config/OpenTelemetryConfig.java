/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.config;

import com.newrelic.api.agent.NewRelic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration for the OpenTelemetry hybrid agent functionality.
 * This config controls which, if any, of the opentelemetry logs, metrics, and traces signals will be captured.
 */
public class OpenTelemetryConfig {

// This is the standard config:
//
//    opentelemetry:
//        enabled: true
//        logs:
//            enabled: true
//        metrics:
//            enabled: true
//            include: "MeterName1,MeterName2"
//            exclude: "MeterName3,MeterName4"
//        traces:
//            enabled: true
//            include: "TracerName1,TracerName2"
//            exclude: "TracerName3,TracerName4"

// This is a legacy config option, kept for backwards compatibility, that was originally used to enable/disable metrics:
//
//    opentelemetry:
//        sdk:
//            autoconfigure:
//                enabled: true

    // We don't have any default excludes, but they should be added here if needed in the future
    public static final Set<String> DEFAULT_METER_EXCLUDES = new HashSet<>();
    public static final Set<String> DEFAULT_TRACER_EXCLUDES = new HashSet<>();

    public static final String COMMA_SEPARATOR = ",";

    public static final String OPENTELEMETRY_ENABLED = "opentelemetry.enabled";
    public static final Boolean OPENTELEMETRY_ENABLED_DEFAULT = false;
    public static final String OPENTELEMETRY_LOGS_ENABLED = "opentelemetry.logs.enabled";
    public static final Boolean OPENTELEMETRY_LOGS_ENABLED_DEFAULT = false;
    public static final String OPENTELEMETRY_METRICS_ENABLED = "opentelemetry.metrics.enabled";
    public static final Boolean OPENTELEMETRY_METRICS_ENABLED_DEFAULT = false;
    public static final String OPENTELEMETRY_TRACES_ENABLED = "opentelemetry.traces.enabled";
    public static final Boolean OPENTELEMETRY_TRACES_ENABLED_DEFAULT = false;
    public static final String OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED = "opentelemetry.sdk.autoconfigure.enabled";
    public static final Boolean OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED_DEFAULT = OPENTELEMETRY_METRICS_ENABLED_DEFAULT;

    public static final String OPENTELEMETRY_METRICS_EXCLUDE = "opentelemetry.metrics.exclude";
    public static final String OPENTELEMETRY_METRICS_INCLUDE = "opentelemetry.metrics.include";
    public static final String OPENTELEMETRY_TRACES_EXCLUDE = "opentelemetry.traces.exclude";
    public static final String OPENTELEMETRY_TRACES_INCLUDE = "opentelemetry.traces.include";

    public static boolean isOpenTelemetrySdkAutoConfigureEnabled() {
        // Legacy setting that was only used to enable SDK exporting of OTel metrics. Kept for backwards compatability. This functioned the same as opentelemetry.enabled now does.
        return NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED, OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED_DEFAULT);
    }

    public static boolean isOpenTelemetryEnabled() {
        return NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_ENABLED, OPENTELEMETRY_ENABLED_DEFAULT);
    }

    public static boolean isOpenTelemetryLogsEnabled() {
        return isOpenTelemetryEnabled() && NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_LOGS_ENABLED, OPENTELEMETRY_LOGS_ENABLED_DEFAULT);
    }

    public static boolean isOpenTelemetryMetricsEnabled() {
        return isOpenTelemetryEnabled() && NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_METRICS_ENABLED, OPENTELEMETRY_METRICS_ENABLED_DEFAULT);
    }

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
        // User configured includes take precedence over default excludes, so we filter them out.
        getOpenTelemetryMetricsIncludes().forEach(DEFAULT_METER_EXCLUDES::remove);

        // Combine the remaining default excludes with the user configured excludes for the final excludes list.
        String metricsExclude = NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_METRICS_EXCLUDE, "");
        List<String> excludedMeters = getUniqueStringsFromString(metricsExclude, COMMA_SEPARATOR);
        excludedMeters.addAll(DEFAULT_METER_EXCLUDES);
        return excludedMeters;
    }

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
        // User configured includes take precedence over default excludes, so we filter them out.
        getOpenTelemetryTracesIncludes().forEach(DEFAULT_TRACER_EXCLUDES::remove);

        // Combine the remaining default excludes with the user configured excludes for the final excludes list.
        String tracesExclude = NewRelic.getAgent().getConfig().getValue(OPENTELEMETRY_TRACES_EXCLUDE, "");
        List<String> excludedTracers = getUniqueStringsFromString(tracesExclude, COMMA_SEPARATOR);
        excludedTracers.addAll(DEFAULT_TRACER_EXCLUDES);
        return excludedTracers;
    }

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
     * Splits the given values String into a collection of Strings based on the provided separator character.
     *
     * @param valuesString A separator delimited string
     * @param separator    A character delimiter in a string
     * @return List of string split by the separator delimiter
     */
    public static List<String> getUniqueStringsFromString(String valuesString, String separator) {
        if (valuesString == null || valuesString.isEmpty()) {
            return Collections.emptyList();
        }
        String[] valuesArray = valuesString.split(separator);
        List<String> result = new ArrayList<>(valuesArray.length);
        for (String value : valuesArray) {
            value = value.trim();
            if (!value.isEmpty() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }
}
