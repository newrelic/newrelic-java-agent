package com.nr.agent.instrumentation.log4j2.layout.template.json;

import com.newrelic.agent.bridge.logging.Log4jUtils;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.logging.log4j.core.LogEvent;

import java.util.Map;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.BLOB_PREFIX;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlobFromMap;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled;

public class AgentUtils {
    /**
     * Checks pretty or compact JSON layout strings for a series of characters and returns the index of
     * the characters or -1 if they were not found. This is used to find the log "message" substring
     * so that the NR-LINKING metadata blob can be inserted when using local decorating with JsonLayout.
     *
     * @param writerString String representing JSON formatted log event
     * @return positive int if index was found, else -1
     */
    public static int getIndexToModifyJson(String writerString) {
        int msgIndex = writerString.indexOf("message");
        if (msgIndex < 0) {
            return msgIndex;
        }
        return writerString.indexOf("\",", msgIndex);
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
