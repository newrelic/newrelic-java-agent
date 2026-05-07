/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.bridge.logging;

import com.newrelic.api.agent.NewRelic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AppLoggingUtils {
    public static final int DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES = 11;
    // Log message attributes
    public static final LogAttributeKey INSTRUMENTATION = new LogAttributeKey("instrumentation", LogAttributeType.AGENT);
    public static final LogAttributeKey MESSAGE = new LogAttributeKey("message", LogAttributeType.AGENT);
    public static final LogAttributeKey TIMESTAMP = new LogAttributeKey("timestamp", LogAttributeType.AGENT);
    public static final LogAttributeKey LEVEL = new LogAttributeKey("level", LogAttributeType.AGENT);
    public static final LogAttributeKey ERROR_MESSAGE = new LogAttributeKey("error.message", LogAttributeType.AGENT);
    public static final LogAttributeKey ERROR_CLASS = new LogAttributeKey("error.class", LogAttributeType.AGENT);
    public static final LogAttributeKey ERROR_STACK = new LogAttributeKey("error.stack", LogAttributeType.AGENT);
    public static final LogAttributeKey THREAD_NAME = new LogAttributeKey("thread.name", LogAttributeType.AGENT);
    public static final LogAttributeKey THREAD_ID = new LogAttributeKey("thread.id", LogAttributeType.AGENT);
    public static final LogAttributeKey LOGGER_NAME = new LogAttributeKey("logger.name", LogAttributeType.AGENT);
    public static final LogAttributeKey LOGGER_FQCN = new LogAttributeKey("logger.fqcn", LogAttributeType.AGENT);
    public static final String UNKNOWN = "UNKNOWN";
    // Linking metadata attributes used in blob
    public static final String BLOB_PREFIX = "NR-LINKING";
    public static final String BLOB_DELIMITER = "|";
    public static final String TRACE_ID = "trace.id";
    public static final String HOSTNAME = "hostname";
    public static final String ENTITY_GUID = "entity.guid";
    public static final String ENTITY_NAME = "entity.name";
    public static final String SPAN_ID = "span.id";
    // Log attribute prefixes
    public static final String CONTEXT_DATA_ATTRIBUTE_PREFIX = "context.";
    // Enabled defaults
    private static final boolean APP_LOGGING_DEFAULT_ENABLED = true;
    private static final boolean APP_LOGGING_METRICS_DEFAULT_ENABLED = true;
    private static final boolean APP_LOGGING_FORWARDING_DEFAULT_ENABLED = true;
    private static final boolean APP_LOGGING_LOCAL_DECORATING_DEFAULT_ENABLED = false;
    private static final boolean APP_LOGGING_FORWARDING_CONTEXT_DATA_DEFAULT_ENABLED = false;

    /**
     * Gets a String representing the agent linking metadata in blob format:
     * NR-LINKING|entity.guid|hostname|trace.id|span.id|entity.name|
     *
     * @return agent linking metadata string blob
     */
    public static String getLinkingMetadataBlob() {
        return constructLinkingMetadataBlob(NewRelic.getAgent().getLinkingMetadata());
    }

    /**
     * Gets a String representing the agent linking metadata in blob format:
     * NR-LINKING|entity.guid|hostname|trace.id|span.id|entity.name|
     *
     * @param agentLinkingMetadata map of linking metadata
     * @return agent linking metadata string blob
     */
    public static String getLinkingMetadataBlobFromMap(Map<String, String> agentLinkingMetadata) {
        return constructLinkingMetadataBlob(agentLinkingMetadata);
    }

    /**
     * Constructs a String representing the agent linking metadata in blob format:
     * NR-LINKING|entity.guid|hostname|trace.id|span.id|entity.name|
     *
     * @param agentLinkingMetadata map of linking metadata
     * @return agent linking metadata string blob
     */
    private static String constructLinkingMetadataBlob(Map<String, String> agentLinkingMetadata) {
        StringBuilder blob = new StringBuilder();
        blob.append(" ").append(BLOB_PREFIX).append(BLOB_DELIMITER);

        if (agentLinkingMetadata != null && !agentLinkingMetadata.isEmpty()) {
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
    public static String urlEncode(String value) {
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
     * Check if the application_logging forwarding context_data feature is enabled.
     *
     * @return true if enabled, else false
     */
    public static boolean isAppLoggingContextDataEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.forwarding.context_data.enabled",
                APP_LOGGING_FORWARDING_CONTEXT_DATA_DEFAULT_ENABLED);
    }
}
