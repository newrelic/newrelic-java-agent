/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.logging.log4j.core.async;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.LinkingMetadataHolder;
import com.newrelic.agent.bridge.logging.Log4jUtils;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LogEvent_Instrumentation;

import java.util.Map;
import java.util.logging.Level;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;

@Weave(originalName = "org.apache.logging.log4j.core.async.RingBufferLogEventTranslator", type = MatchType.ExactClass)
public class RingBufferLogEventTranslator_Instrumentation {
    public void translateTo(final RingBufferLogEvent event, final long sequence) {
        if (isApplicationLoggingEnabled()) {
            LinkingMetadataHolder holder = new LinkingMetadataHolder(AgentBridge.getAgent().getLinkingMetadata());
            NewRelic.getAgent().getLogger().log(Level.INFO, "ringbuffer: {0}", System.identityHashCode(event));
            NewRelic.getAgent().getLogger().log(Level.INFO, "ringbuffer: {0}", NewRelic.getAgent().getTransaction());
            NewRelic.getAgent().getLogger().log(Level.INFO, "ringbuffer: {0}", holder.getLinkingMetadata());
            if (holder.isValid()) {
                Log4jUtils.addLinkingMetadataToCache(event, holder);
            }
        }

        Weaver.callOriginal();
    }
}
