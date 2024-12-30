/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.log4j2.layout.template.json;

import com.newrelic.agent.bridge.logging.Log4jUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.BLOB_PREFIX;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlobFromMap;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled;

public class AgentUtils {
    // Detect the end of the "message" field when it's not the last field -- ",
    private static final Pattern JSON_MESSAGE_VALUE_END = Pattern.compile("\"message\".+?[^\\\\](\",)");
    // Detect the end of the "message" field when it is the last field -- "}
    private static final Pattern JSON_MESSAGE_VALUE_END2 = Pattern.compile("\"message\".+?(\"})");

    /**
     * Checks pretty or compact JSON layout strings for a series of characters and returns the index of
     * the characters or -1 if they were not found. This is used to find the log "message" substring
     * so that the NR-LINKING metadata blob can be inserted when using local decorating with JsonTemplateLayout.
     *
     * @param writerString String representing JSON formatted log event
     * @return positive int if index was found, else -1
     */
    public static int getIndexToModifyJson(String writerString) {
        int index = -1;
        Matcher matcher = JSON_MESSAGE_VALUE_END.matcher(writerString);

        if (matcher.find()) {
            // Group 1 in the match is the ", char sequence
            index = matcher.start(1);
        } else {
            matcher = JSON_MESSAGE_VALUE_END2.matcher(writerString);
            if (matcher.find()) {
                // Group 1 in the match is "}
                index = matcher.start(1);
            }
        }

        return index;
    }

    /**
     * Check if a valid match was found when calling String.indexOf.
     * If index value is -1 then no valid match was found, a positive integer represents a valid index.
     *
     * @param indexToModifyJson int representing index returned by indexOf
     * @return true if a valid index was found, else false
     */
    public static boolean foundIndexToInsertLinkingMetadata(int indexToModifyJson) {
        return indexToModifyJson != -1;
    }

    /**
     * Updates {@code jsonStringBuilder} with agent linking metadata.
     *
     * @param event LogEvent that may contain linking metadata
     * @param jsonStringBuilder JSON formatted string builder that will be modified with linking metadata
     */
    public static void writeLinkingMetadata(final LogEvent event, StringBuilder jsonStringBuilder) {
        String oldJsonString = jsonStringBuilder.toString();
        // Append linking metadata to the log message if local decorating is enabled
        if (isApplicationLoggingEnabled() && isApplicationLoggingLocalDecoratingEnabled()) {
            // It is possible that the log might already have NR-LINKING metadata from JUL instrumentation
            if (!oldJsonString.contains(BLOB_PREFIX)) {
                int indexToModifyJson = getIndexToModifyJson(oldJsonString);
                if (foundIndexToInsertLinkingMetadata(indexToModifyJson)) {
                    Map<String, String> linkingMetadata = Log4jUtils.getLinkingMetadata(event);
                    // Replace the JSON string with modified version that includes NR-LINKING metadata
                    if (event != null && linkingMetadata != null && (!linkingMetadata.isEmpty())) {
                        // Get linking metadata stored on LogEvent if available. This ensures that
                        // the trace.id and span.id will be available when using an async appender.
                        jsonStringBuilder.insert(indexToModifyJson, getLinkingMetadataBlobFromMap(linkingMetadata));
                    } else {
                        // Get linking metadata from current thread if it is not available on LogEvent.
                        jsonStringBuilder.insert(indexToModifyJson, getLinkingMetadataBlob());
                    }
                }
            }
        }
    }
}
