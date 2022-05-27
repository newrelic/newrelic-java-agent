/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.logbackclassic12;

import ch.qos.logback.classic.Level;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AgentUtil {
    public static final int DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES = 10;
    // Log message attributes
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String LEVEL = "level";
    public static final String ERROR_MESSAGE = "error.message";
    public static final String ERROR_CLASS = "error.class";
    public static final String ERROR_STACK = "error.stack";
    public static final String THREAD_NAME = "thread.name";
    public static final String THREAD_ID = "thread.id";
    public static final String LOGGER_NAME = "logger.name";
    public static final String LOGGER_FQCN = "logger.fqcn";
    public static final String UNKNOWN = "UNKNOWN";

    // Linking metadata attributes used in blob
    private static final String BLOB_PREFIX = "NR-LINKING";
    private static final String BLOB_DELIMITER = "|";
    private static final String TRACE_ID = "trace.id";
    private static final String HOSTNAME = "hostname";
    private static final String ENTITY_GUID = "entity.guid";
    private static final String ENTITY_NAME = "entity.name";
    private static final String SPAN_ID = "span.id";
    // Log attribute prefixes
    private static final String MDC_ATTRIBUTE_PREFIX = "mdc.";
    // Enabled defaults
    private static final boolean APP_LOGGING_DEFAULT_ENABLED = true;
    private static final boolean APP_LOGGING_METRICS_DEFAULT_ENABLED = true;
    private static final boolean APP_LOGGING_FORWARDING_DEFAULT_ENABLED = true;
    private static final boolean APP_LOGGING_LOCAL_DECORATING_DEFAULT_ENABLED = false;
    private static final boolean APP_LOGGING_FORWARDING_INCLUDE_MDC_DEFAULT_ENABLED = false;

    /**
     * Record a LogEvent to be sent to New Relic.
     *
     * @param message         log message
     * @param timeStampMillis log timestamp
     * @param level           log level
     */
    public static void recordNewRelicLogEvent(String message, Map<String, String> mdcPropertyMap, long timeStampMillis, Level level, Throwable throwable, String threadName, long threadId,
            String loggerName, String fqcnLoggerName) {
        boolean messageEmpty = message.isEmpty();

        if (shouldCreateLogEvent(messageEmpty, throwable)) {
            HashMap<String, Object> logEventMap = new HashMap<>(DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES);

            if (!messageEmpty) {
                logEventMap.put(MESSAGE, message);
            }
            logEventMap.put(TIMESTAMP, timeStampMillis);

            if (isApplicationLoggingForwardingIncludeMdcEnabled()) {
                mdcPropertyMap.forEach((key, value) -> logEventMap.put(MDC_ATTRIBUTE_PREFIX + key, value));
            }

            if (level.toString().isEmpty()) {
                logEventMap.put(LEVEL, UNKNOWN);
            } else {
                logEventMap.put(LEVEL, level);
            }

            String errorStack = ExceptionUtil.getErrorStack(throwable);
            if (errorStack != null) {
                logEventMap.put(ERROR_STACK, errorStack);
            }

            String errorMessage = ExceptionUtil.getErrorMessage(throwable);
            if (errorMessage != null) {
                logEventMap.put(ERROR_MESSAGE, errorMessage);
            }

            String errorClass = ExceptionUtil.getErrorClass(throwable);
            if (errorClass != null) {
                logEventMap.put(ERROR_CLASS, errorClass);
            }

            if (threadName != null) {
                logEventMap.put(THREAD_NAME, threadName);
            }

            logEventMap.put(THREAD_ID, threadId);

            if (loggerName != null) {
                logEventMap.put(LOGGER_NAME, loggerName);
            }

            if (fqcnLoggerName != null) {
                logEventMap.put(LOGGER_FQCN, fqcnLoggerName);
            }

            AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
        }
    }

    /**
     * A LogEvent MUST NOT be reported if neither a log message nor an error is logged. If either is present report the LogEvent.
     *
     * @param messageEmpty Message to validate
     * @param throwable    Throwable to validate
     * @return true if a LogEvent should be created, otherwise false
     */
    private static boolean shouldCreateLogEvent(boolean messageEmpty, Throwable throwable) {
        return !messageEmpty || !ExceptionUtil.isThrowableNull(throwable);
    }

    private static int getDefaultNumOfLogEventAttributes(Map<String, String> mdcPropertyMap) {
        return isApplicationLoggingForwardingIncludeMdcEnabled()
                ? mdcPropertyMap.size() + DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES
                : DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES;
    }

    /**
     * Gets a String representing the agent linking metadata in blob format:
     * NR-LINKING|entity.guid|hostname|trace.id|span.id|entity.name|
     *
     * @return agent linking metadata string blob
     */
    public static String getLinkingMetadataBlob() {
        Map<String, String> agentLinkingMetadata = NewRelic.getAgent().getLinkingMetadata();
        StringBuilder blob = new StringBuilder();
        blob.append(" ").append(BLOB_PREFIX).append(BLOB_DELIMITER);

        if (agentLinkingMetadata != null && agentLinkingMetadata.size() > 0) {
            appendAttributeToBlob(agentLinkingMetadata.get(ENTITY_GUID), blob);
            appendAttributeToBlob(agentLinkingMetadata.get(HOSTNAME), blob);
            appendAttributeToBlob(agentLinkingMetadata.get(TRACE_ID), blob);
            appendAttributeToBlob(agentLinkingMetadata.get(SPAN_ID), blob);
            appendAttributeToBlob(urlEncode(agentLinkingMetadata.get(ENTITY_NAME)), blob);
        }
        return blob.toString();
    }

    private static void appendAttributeToBlob(String attribute, StringBuilder blob) {
        if (attribute != null && !attribute.isEmpty()) {
            blob.append(attribute);
        }
        blob.append(BLOB_DELIMITER);
    }

    /**
     * URL encode a String value.
     *
     * @param value String to encode
     * @return URL encoded String
     */
    static String urlEncode(String value) {
        try {
            if (value != null) {
                value = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
            }
        } catch (UnsupportedEncodingException e) {
            NewRelic.getAgent()
                    .getLogger()
                    .log(java.util.logging.Level.WARNING, "Unable to URL encode entity.name for application_logging.local_decorating", e);
        }
        return value;
    }

    /**
     * Check if all application_logging features are enabled.
     *
     * @return true if enabled, else false
     */
    public static boolean isApplicationLoggingEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.enabled", APP_LOGGING_DEFAULT_ENABLED);
    }

    /**
     * Check if the application_logging metrics feature is enabled.
     *
     * @return true if enabled, else false
     */
    public static boolean isApplicationLoggingMetricsEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.metrics.enabled", APP_LOGGING_METRICS_DEFAULT_ENABLED);
    }

    /**
     * Check if the application_logging forwarding feature is enabled.
     *
     * @return true if enabled, else false
     */
    public static boolean isApplicationLoggingForwardingEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.forwarding.enabled", APP_LOGGING_FORWARDING_DEFAULT_ENABLED);
    }

    /**
     * Check if the application_logging local_decorating feature is enabled.
     *
     * @return true if enabled, else false
     */
    public static boolean isApplicationLoggingLocalDecoratingEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.local_decorating.enabled", APP_LOGGING_LOCAL_DECORATING_DEFAULT_ENABLED);
    }

    /**
     * Check if the application_logging forwarding include_mdc feature is enabled.
     *
     * @return true if enabled, else false
     */
    public static boolean isApplicationLoggingForwardingIncludeMdcEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.forwarding.include_mdc.enabled", APP_LOGGING_FORWARDING_INCLUDE_MDC_DEFAULT_ENABLED);
    }
}
