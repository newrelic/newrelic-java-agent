/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.logging.log4j.spi;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.Message;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.*;

@Weave(originalName = "org.apache.logging.log4j.spi.AbstractLogger", type = MatchType.BaseClass)
public abstract class AbstractLogger_Instrumentation {
    private void logMessageSafely(final String fqcn, final Level level, final Marker marker,
            final Message message, final Throwable throwable) {
        boolean addedLinkingMetadata = false;
        if (isApplicationLoggingEnabled() && isApplicationLoggingLocalDecoratingEnabled()) {
            ThreadContext.put(BLOB_PREFIX, getLinkingMetadataBlob());
            addedLinkingMetadata = true;
        }

        try {
            Weaver.callOriginal();
        } finally {
            if (addedLinkingMetadata) {
                ThreadContext.remove(BLOB_PREFIX);
            }
        }
    }
}
