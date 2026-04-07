/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.opentelemetry;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.ErrorGroupCallback;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.TransactionNamePriority;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class OpenTelemetryNewRelic {

    private static final OpenTelemetryAgent NOOP_AGENT = OpenTelemetryAgent.create(OpenTelemetry.noop());
    static Logger LOGGER = Logger.getLogger(OpenTelemetryNewRelic.class.getName());
    // TODO: add scope version
    static String SCOPE_NAME = "com.newrelic.opentelemetry-bridge";

    private static final AtomicReference<OpenTelemetryAgent> AGENT_REF = new AtomicReference<>(NOOP_AGENT);

    public static Agent getAgent() {
        return AGENT_REF.get();
    }

    // Visible for testing
    public static void resetForTest() {
        AGENT_REF.set(NOOP_AGENT);
    }

    public static void install(OpenTelemetry openTelemetry) {
        AGENT_REF.set(OpenTelemetryAgent.create(openTelemetry));
    }

    // ************************** Metric API ****************************************//

    public static void recordMetric(String name, float value) {
        getAgent().getMetricAggregator().recordMetric(name, value);
    }

    public static void recordResponseTimeMetric(String name, long millis) {
        getAgent().getMetricAggregator().recordResponseTimeMetric(name, millis);
    }

    public static void incrementCounter(String name) {
        getAgent().getMetricAggregator().incrementCounter(name);
    }

    public static void incrementCounter(String name, int count) {
        getAgent().getMetricAggregator().incrementCounter(name, count);
    }

    // ************************** Error API ***********************************//

    public static void noticeError(Throwable throwable, Map<String, ?> params) {
        getAgent().getErrorApi().noticeError(throwable, params);
    }

    public static void noticeError(Throwable throwable) {
        getAgent().getErrorApi().noticeError(throwable);
    }

    public static void noticeError(String message, Map<String, ?> params) {
        getAgent().getErrorApi().noticeError(message, params);
    }

    public static void noticeError(String message) {
        getAgent().getErrorApi().noticeError(message);
    }

    public static void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
        getAgent().getErrorApi().noticeError(throwable, params, expected);
    }

    public static void noticeError(Throwable throwable, boolean expected) {
        getAgent().getErrorApi().noticeError(throwable, expected);
    }

    public static void noticeError(String message, Map<String, ?> params, boolean expected) {
        getAgent().getErrorApi().noticeError(message, params, expected);
    }

    public static void noticeError(String message, boolean expected) {
        getAgent().getErrorApi().noticeError(message, expected);
    }

    public static void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback) {
        getAgent().getErrorApi().setErrorGroupCallback(errorGroupCallback);
    }

    // **************************** Transaction APIs ********************************//

    public static void addCustomParameter(String key, Number value) {
        logUnsupportedMethod("NewRelic", "addCustomParameter");
    }

    public static void addCustomParameter(String key, String value) {
        logUnsupportedMethod("NewRelic", "addCustomParameter");
    }

    public static void addCustomParameter(String key, boolean value) {
        logUnsupportedMethod("NewRelic", "addCustomParameter");
    }

    public static void addCustomParameters(Map<String, Object> params) {
        logUnsupportedMethod("NewRelic", "addCustomParameters");
    }

    public static void setUserId(String userId) {
        logUnsupportedMethod("NewRelic", "setUserId");
    }

    public static void setTransactionName(String category, String name) {
        getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_LOW, true, category, name);
    }

    public static void ignoreTransaction() {
        getAgent().getTransaction().ignore();
    }

    public static void ignoreApdex() {
        getAgent().getTransaction().ignoreApdex();
    }

    public static void setRequestAndResponse(Request request, Response response) {
        logUnsupportedMethod("NewRelic", "setRequestAndResponse");
    }

    // **************************** Real User Monitoring (RUM) ********************************
    // API calls to support manual instrumentation of RUM if auto instrumentation is not available.
    // Get the JavaScript header and footer that should be inserted into the HTML response for browser-side monitoring.

    public static String getBrowserTimingHeader() {
        logUnsupportedMethod("NewRelic", "getBrowserTimingHeader");
        return "";
    }

    public static String getBrowserTimingHeader(String nonce) {
        logUnsupportedMethod("NewRelic", "getBrowserTimingHeader");
        return "";
    }

    public static void setUserName(String name) {
        logUnsupportedMethod("NewRelic", "setUserName");
    }

    public static void setAccountName(String name) {
        logUnsupportedMethod("NewRelic", "setAccountName");
    }

    public static void setProductName(String name) {
        logUnsupportedMethod("NewRelic", "setProductName");
    }

    // **************************** Web Frameworks API ********************************

    public static void setAppServerPort(int port) {
        logUnsupportedMethod("NewRelic", "setAppServerPort");
    }

    public static void setServerInfo(String dispatcherName, String version) {
        logUnsupportedMethod("NewRelic", "setServerInfo");
    }

    public static void setInstanceName(String instanceName) {
        logUnsupportedMethod("NewRelic", "setInstanceName");
    }

    // Internal helpers

    static void logUnsupportedMethod(String className, String methodName) {
        // If FINER or FINEST is enabled, indicate that an unsupported method was called.
        // If FINEST is enabled, include stacktrace.
        if (LOGGER.isLoggable(Level.FINEST)) {
            OpenTelemetryNewRelic.LOGGER.log(Level.FINEST, "NewRelic API "
                    + className + "#" + methodName
                    + " was called but is not supported by OpenTelemetryNewRelic bridge. "
                    + "Throwable points to code that called method.", new Throwable());
        } else if (LOGGER.isLoggable(Level.FINE)) {
            OpenTelemetryNewRelic.LOGGER.log(Level.FINE, "NewRelic API "
                    + className + "#" + methodName
                    + " was called but is not supported by OpenTelemetryNewRelic bridge.");
        }
    }

    static AttributesBuilder toAttributes(Map<String, ?> attributesMap) {
        AttributesBuilder builder = io.opentelemetry.api.common.Attributes.builder();

        attributesMap.forEach(
                (key, value) -> {
                    if (value instanceof Double) {
                        builder.put(key, (Double) value);
                    } else if (value instanceof Float) {
                        builder.put(key, ((Float) value).doubleValue());
                    } else if (value instanceof Long) {
                        builder.put(key, (Long) value);
                    } else if (value instanceof Number) {
                        builder.put(key, ((Number) value).longValue());
                    } else if (value instanceof Boolean) {
                        builder.put(key, (Boolean) value);
                    } else {
                        builder.put(key, value.toString());
                    }
                });

        return builder;
    }

}
