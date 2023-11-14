/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.logging.log4j.core.layout;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.BLOB_PREFIX;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled;

@Weave(originalName = "org.apache.logging.log4j.core.layout.StringBuilderEncoder", type = MatchType.BaseClass)
public class StringBuilderEncoder_Instrumentation {

    public void encode(final StringBuilder source, final ByteBufferDestination destination) {
        // Do nothing if application_logging.enabled: false
        if (isApplicationLoggingEnabled()) {
            if (isApplicationLoggingLocalDecoratingEnabled()) {
                // Append New Relic linking metadata from agent to log message
                appendAgentMetadata(source);
            }
        }
        Weaver.callOriginal();
    }

    private void appendAgentMetadata(StringBuilder source) {
        String sourceString = source.toString();
        // It is possible that the log might already have NR-LINKING metadata from JUL instrumentation
        if (!sourceString.contains(BLOB_PREFIX)) {
            int breakLine = sourceString.lastIndexOf("\n");
            if (breakLine != -1) {
                source.replace(breakLine, breakLine + 1, "");
            }
            source.append(getLinkingMetadataBlob()).append("\n");
        }
    }

}
