/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.logging.log4j.core.layout;

import com.newrelic.agent.bridge.logging.LinkingMetadataHolder;
import com.newrelic.agent.bridge.logging.Log4jUtils;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.logging.log4j.core.LogEvent_Instrumentation;

import java.util.Map;
import java.util.logging.Level;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.BLOB_PREFIX;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlobFromMap;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled;

@Weave(originalName = "org.apache.logging.log4j.core.layout.PatternLayout", type = MatchType.ExactClass)
public abstract class PatternLayout_Instrumentation {
//    public void encode(final LogEvent_Instrumentation event, final ByteBufferDestination destination) {
//        String originalLog;
//        StringBuilder stringBuilder;
//
//        originalLog = Weaver.callOriginal();
//        stringBuilder = new StringBuilder(originalLog);
//
//        NewRelic.getAgent().getLogger().log(Level.INFO, "originalLog: ", originalLog);
//
//        // Append linking metadata to the log message if local decorating is enabled
//        if (isApplicationLoggingEnabled() && isApplicationLoggingLocalDecoratingEnabled()) {
//            appendAgentMetadata(stringBuilder, event);
//        }
//    }

    private StringBuilder toText(final AbstractStringLayout.Serializer2 serializer, final LogEvent_Instrumentation event, final StringBuilder destination) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "toText0 - {0}", serializer);
        NewRelic.getAgent().getLogger().log(Level.INFO, "toText0 - {0}", event);

        StringBuilder originalLogStringBuilder = Weaver.callOriginal();

        NewRelic.getAgent().getLogger().log(Level.INFO, "toText - {0}", originalLogStringBuilder);
        NewRelic.getAgent().getLogger().log(Level.INFO, "toText - {0}", event.getThreadId());
        NewRelic.getAgent().getLogger().log(Level.INFO, "toText - {0}", Log4jUtils.getLinkingMetadataFromCache(event));

        // Append linking metadata to the log message if local decorating is enabled
        if (originalLogStringBuilder != null && isApplicationLoggingEnabled() && isApplicationLoggingLocalDecoratingEnabled()) {
            appendAgentMetadata(originalLogStringBuilder, event);
        }

        NewRelic.getAgent().getLogger().log(Level.INFO, "toText2 - {0}", originalLogStringBuilder);

        return originalLogStringBuilder;
    }

    private void appendAgentMetadata(StringBuilder source, LogEvent_Instrumentation event) {
        String sourceString = source.toString();
        // It is possible that the log might already have NR-LINKING metadata from JUL instrumentation
        if (!sourceString.contains(BLOB_PREFIX)) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "toText3 - in append blob");
            LinkingMetadataHolder agentLinkingMetadata = Log4jUtils.getLinkingMetadataFromCache(event);
            NewRelic.getAgent().getLogger().log(Level.INFO, "toText3 - got metadata???  {0}", agentLinkingMetadata);
            int breakLine = sourceString.lastIndexOf("\n");
            NewRelic.getAgent().getLogger().log(Level.INFO, "toText3 - breakLine  {0}", breakLine);
            if (breakLine != -1) {
                source.replace(breakLine, breakLine + 1, "");
            }

            if (agentLinkingMetadata.isValid()) {
                source.append(getLinkingMetadataBlobFromMap(agentLinkingMetadata.getLinkingMetadata())).append("\n");
            }
        }
    }
}
