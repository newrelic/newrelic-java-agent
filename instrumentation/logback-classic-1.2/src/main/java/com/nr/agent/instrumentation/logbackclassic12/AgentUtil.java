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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AgentUtil {
    // Log message attributes
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String LOG_LEVEL = "log.level";
    public static final String UNKNOWN = "UNKNOWN";
    // Linking metadata attributes to filter out
    private static final String ENTITY_TYPE = "entity.type";
    private static final String ENTITY_NAME = "entity.name";
    // Enabled defaults
    private static final boolean APP_LOGGING_DEFAULT_ENABLED = false;
    private static final boolean APP_LOGGING_METRICS_DEFAULT_ENABLED = false;
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
            HashMap<String, Object> logEventMap = new HashMap<>(getFilteredLinkingMetadataMap());
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
     * Gets a String representing the agent linking metadata after filtering
     * out entity.type, entity.name, and any attributes with an empty value.
     *
     * @return Filtered String of agent linking metadata
     */
    public static String getFilteredLinkingMetadataString() {
        return getFilteredLinkingMetadataMap().toString();
    }

    /**
     * Gets a map of agent linking metadata after filtering out
     * entity.type, entity.name, and any attributes with an empty value.
     *
     * @return Filtered map of agent linking metadata
     */
    public static Map<String, String> getFilteredLinkingMetadataMap() {
        Map<String, String> agentLinkingMetadata = NewRelic.getAgent().getLinkingMetadata();

        if (agentLinkingMetadata != null && agentLinkingMetadata.size() > 0) {
            Map<String, String> map = new HashMap<>();
            Set<Map.Entry<String, String>> metadataSet = agentLinkingMetadata.entrySet();

            for (Map.Entry<String, String> entry : metadataSet) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!key.equals(ENTITY_NAME) && !key.equals(ENTITY_TYPE) && !value.isEmpty()) {
                    map.put(key, value);
                }
            }
            return map;
        } else {
            return Collections.emptyMap();
        }
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
