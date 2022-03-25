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
    public static final int DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES = 3;
    // Log message attributes
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String LOG_LEVEL = "log.level";
    public static final String UNKNOWN = "UNKNOWN";
    // Linking metadata attributes used in blob
    private static final String BLOB_PREFIX = "NR-LINKING";
    private static final String BLOB_DELIMITER = "|";
    private static final String TRACE_ID = "trace.id";
    private static final String HOSTNAME = "hostname";
    private static final String ENTITY_GUID = "entity.guid";
    private static final String ENTITY_NAME = "entity.name";
    private static final String SPAN_ID = "span.id";
    // Enabled defaults
    private static final boolean APP_LOGGING_DEFAULT_ENABLED = true;
    private static final boolean APP_LOGGING_METRICS_DEFAULT_ENABLED = true;
    private static final boolean APP_LOGGING_FORWARDING_DEFAULT_ENABLED = false;
    private static final boolean APP_LOGGING_LOCAL_DECORATING_DEFAULT_ENABLED = false;

    /**
     * Record a LogEvent to be sent to New Relic.
     *
     * @param message         log message
     * @param timeStampMillis log timestamp
     * @param level           log level
     */
    public static void recordNewRelicLogEvent(String message, long timeStampMillis, Level level) {
        // Bail out and don't create a LogEvent if log message is empty
        if (!message.isEmpty()) {
            HashMap<String, Object> logEventMap = new HashMap<>(DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES);
            logEventMap.put(MESSAGE, message);
            logEventMap.put(TIMESTAMP, timeStampMillis);

            if (level.toString().isEmpty()) {
                logEventMap.put(LOG_LEVEL, UNKNOWN);
            } else {
                logEventMap.put(LOG_LEVEL, level);
            }

            AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
        }
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
            NewRelic.getAgent().getLogger().log(java.util.logging.Level.WARNING, "Unable to URL encode entity.name for application_logging.local_decorating", e);
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
}
