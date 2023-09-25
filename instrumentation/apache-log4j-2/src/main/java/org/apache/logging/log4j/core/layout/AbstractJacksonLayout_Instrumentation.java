/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.logging.log4j.core.layout;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.logging.log4j.core.LogEvent_Instrumentation;
import org.apache.logging.log4j.core.util.StringBuilderWriter;

import java.io.IOException;
import java.io.Writer;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.BLOB_PREFIX;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlobFromMap;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled;
import static com.nr.agent.instrumentation.log4j2.AgentUtil.foundIndexToInsertLinkingMetadata;
import static com.nr.agent.instrumentation.log4j2.AgentUtil.getIndexToModifyJson;

@Weave(originalName = "org.apache.logging.log4j.core.layout.AbstractJacksonLayout", type = MatchType.ExactClass)
abstract class AbstractJacksonLayout_Instrumentation {

    public abstract void toSerializable(final LogEvent_Instrumentation event, final Writer writer) throws IOException;

    public String toSerializable(final LogEvent_Instrumentation event) {
        final StringBuilderWriter writer = new StringBuilderWriter();
        try {
            toSerializable(event, writer);
            String writerString = writer.toString();
            String modified = writerString;

            // Append linking metadata to the log message if local decorating is enabled
            if (isApplicationLoggingEnabled() && isApplicationLoggingLocalDecoratingEnabled()) {
                // It is possible that the log might already have NR-LINKING metadata from JUL instrumentation
                if (!writerString.contains(BLOB_PREFIX)) {
                    int indexToModifyJson = getIndexToModifyJson(writerString);
                    if (foundIndexToInsertLinkingMetadata(indexToModifyJson)) {
                        // Replace the JSON string with modified version that includes NR-LINKING metadata
                        if (event != null && event.agentLinkingMetadata != null && (!event.agentLinkingMetadata.isEmpty())) {
                            // Get linking metadata stored on LogEvent if available. This ensures that
                            // the trace.id and span.id will be available when using an async appender.
                            modified = new StringBuilder(writerString).insert(indexToModifyJson, getLinkingMetadataBlobFromMap(event.agentLinkingMetadata))
                                    .toString();
                        } else {
                            // Get linking metadata from current thread if it is not available on LogEvent.
                            modified = new StringBuilder(writerString).insert(indexToModifyJson, getLinkingMetadataBlob()).toString();
                        }
                    }
                }
                return modified;
            }

            return writerString;
        } catch (final IOException e) {
            return Weaver.callOriginal();
        }
    }

}
