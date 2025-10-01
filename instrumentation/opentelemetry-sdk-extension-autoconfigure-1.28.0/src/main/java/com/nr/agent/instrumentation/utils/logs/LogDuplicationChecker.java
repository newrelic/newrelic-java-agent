/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.logs;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.NewRelic;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Utility class to help prevent duplicate log events when both OpenTelemetry logging framework
 * instrumentation and New Relic weave instrumentation of the same logging framework are enabled.
 */
public class LogDuplicationChecker {
    // These are known classes from standalone OpenTelemetry instrumentation of logging frameworks that we also weave.
    private static final String LOGBACK_OTEL_INSTRUMENTATION_CLASS = "io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender";
    private static final String LOG4J_OTEL_INSTRUMENTATION_CLASS = "io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender";

    // Static flags to indicate if known OpenTelemetry log framework instrumentation is present on the classpath.
    private static final AtomicBoolean logbackOTelInstrumentationInstalled = isOTelLogFrameworkInstrumentationInstalled(LOGBACK_OTEL_INSTRUMENTATION_CLASS);
    private static final AtomicBoolean log4jOTelInstrumentationInstalled = isOTelLogFrameworkInstrumentationInstalled(LOG4J_OTEL_INSTRUMENTATION_CLASS);

    // These are the names of our weave instrumentation modules for logging frameworks that OTel also instruments.
    private static final String LOGBACK_WEAVE_INSTRUMENTATION_NAME = "logback-classic-1.2";
    private static final String LOG4J_WEAVE_INSTRUMENTATION_NAME = "apache-log4j-2.11";

    // Static flags to indicate if we have weave instrumentation enabled for the corresponding logging framework.
    private static final AtomicBoolean logbackWeaveInstrumentationEnabled = isLogFrameworkWeaveInstrumentationEnabled(LOGBACK_WEAVE_INSTRUMENTATION_NAME);
    private static final AtomicBoolean log4jWeaveInstrumentationEnabled = isLogFrameworkWeaveInstrumentationEnabled(LOG4J_WEAVE_INSTRUMENTATION_NAME);

    /**
     * Check if a specific OpenTelemetry log framework instrumentation class is present on the classpath.
     *
     * @param otelInstrumentationClass to check (e.g. io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender)
     * @return AtomicBoolean true if the class is present, otherwise false
     */
    private static AtomicBoolean isOTelLogFrameworkInstrumentationInstalled(String otelInstrumentationClass) {
        try {
            Class.forName(otelInstrumentationClass);
        } catch (ClassNotFoundException __) {
            return new AtomicBoolean(false);
        }
        Agent.LOG.log(Level.WARNING, "Detected " + otelInstrumentationClass +
                " on the classpath. This may result in duplicate log events if the corresponding New Relic logging framework instrumentation module is also enabled");
        return new AtomicBoolean(true);
    }

    /**
     * Check if a weave instrumentation module is enabled for a specific logging framework.
     *
     * @param weaveModuleName to check (e.g. logback-classic-1.2, apache-log4j-2.11)
     * @return AtomicBoolean true if the weave module is enabled, otherwise false
     */
    private static AtomicBoolean isLogFrameworkWeaveInstrumentationEnabled(String weaveModuleName) {
        final boolean weaveModuleEnabled = NewRelic.getAgent()
                .getConfig()
                .getValue("class_transformer.com.newrelic.instrumentation." + weaveModuleName + ".enabled", true);
        return new AtomicBoolean(weaveModuleEnabled);
    }

    /**
     * Inspect the stack trace to determine if a log originated from a known OpenTelemetry
     * log framework instrumentation class.
     *
     * @param otelInstrumentationClass known OTel log framework instrumentation class to search for in the stack trace
     * @return true if the log originated from the specified OTel log framework instrumentation, otherwise false
     */
    private static boolean isLogFromOTelLogFrameworkInstrumentation(String otelInstrumentationClass) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().toLowerCase().contains(otelInstrumentationClass.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Log a message indicating that a LogEvent will not be created from OpenTelemetry
     * log framework instrumentation to avoid duplication with the corresponding New Relic
     * weave instrumentation module.
     *
     * @param otelInstrumentationClass known OTel log framework instrumentation class (e.g. io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender)
     * @param weaveInstrumentationName corresponding New Relic weave instrumentation module name (e.g. logback-classic-1.2)
     */
    private static void logEventSkippedMessage(String otelInstrumentationClass, String weaveInstrumentationName) {
        NewRelic.getAgent()
                .getLogger()
                .log(Level.FINEST,
                        "Skipped creating a LogEvent from " + otelInstrumentationClass + " to avoid duplication with the " +
                                weaveInstrumentationName + " instrumentation module. The " + weaveInstrumentationName +
                                " instrumentation module must be disabled to defer to the OpenTelemetry instrumentation.");

    }

    /**
     * Determine if a LogEvent should be created from an OpenTelemetry log based
     * on inspecting the stack trace to determine if it originated from known
     * OTel of a logging framework that we also weave.
     *
     * @return true if a LogEvent should be created, otherwise false
     */
    public static boolean shouldRecordLogFromOTelAPI() {
        // No OTel log framework instrumentation is installed, so duplication is not a concern.
        // Allow the LogEvent to be created from the OTel API.
        if (!logbackOTelInstrumentationInstalled.get() && !log4jOTelInstrumentationInstalled.get()) {
            return true;
        }

        // Check if OTel logback framework instrumentation is installed.
        if (logbackOTelInstrumentationInstalled.get()) {
            // OTel logback framework instrumentation is installed, so check if our corresponding weave instrumentation is disabled.
            if (!logbackWeaveInstrumentationEnabled.get()) {
                // Our weave instrumentation is disabled, so create the LogEvent from the OTel API.
                return true;
            } else {
                // Our weave instrumentation is enabled, we need to potentially prevent duplicated logs.
                if (isLogFromOTelLogFrameworkInstrumentation(LOGBACK_OTEL_INSTRUMENTATION_CLASS)) {
                    // If the log originated from OTel logback framework instrumentation, it should be skipped to avoid duplication.
                    logEventSkippedMessage(LOGBACK_OTEL_INSTRUMENTATION_CLASS, LOGBACK_WEAVE_INSTRUMENTATION_NAME);
                    return false;
                }
            }
        }

        // Check if OTel log4j framework instrumentation is installed.
        if (log4jOTelInstrumentationInstalled.get()) {
            // OTel log4j framework instrumentation is installed, so check if our corresponding weave instrumentation is disabled.
            if (!log4jWeaveInstrumentationEnabled.get()) {
                // Our weave instrumentation is disabled, so create the LogEvent from the OTel API.
                return true;
            } else {
                // Our weave instrumentation is enabled, we need to potentially prevent duplicated logs.
                if (isLogFromOTelLogFrameworkInstrumentation(LOG4J_OTEL_INSTRUMENTATION_CLASS)) {
                    // If the log originated from OTel log4j framework instrumentation, it should be skipped to avoid duplication.
                    logEventSkippedMessage(LOG4J_OTEL_INSTRUMENTATION_CLASS, LOG4J_WEAVE_INSTRUMENTATION_NAME);
                    return false;
                }
            }
        }
        // If the log doesn't originate from a known OTel instrumentation source, record the LogEvent.
        return true;
    }
}
