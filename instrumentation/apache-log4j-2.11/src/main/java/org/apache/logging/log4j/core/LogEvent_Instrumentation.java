/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.logging.log4j.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.LinkingMetadataHolder;
import com.newrelic.agent.bridge.logging.Log4jUtils;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import java.util.Map;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled;

@Weave(originalName = "org.apache.logging.log4j.core.LogEvent", type = MatchType.Interface)
public abstract class LogEvent_Instrumentation {

    @WeaveAllConstructors
    public LogEvent_Instrumentation() {
        if (isApplicationLoggingEnabled()) {
            LinkingMetadataHolder holder = new LinkingMetadataHolder(AgentBridge.getAgent().getLinkingMetadata());
            if (holder.isValid()) {
                Log4jUtils.addLinkingMetadataToCache(this, holder);
            }
        }
    }

    public abstract LogEvent toImmutable();

    @Deprecated
    public abstract Map<String, String> getContextMap();

    public abstract ReadOnlyStringMap getContextData();

    public abstract ThreadContext.ContextStack getContextStack();

    public abstract String getLoggerFqcn();

    public abstract Level getLevel();

    public abstract String getLoggerName();

    public abstract Marker getMarker();

    public abstract Message getMessage();

    public abstract long getTimeMillis();

    public abstract Instant getInstant();

    public abstract StackTraceElement getSource();

    public abstract String getThreadName();

    public abstract long getThreadId();

    public abstract int getThreadPriority();

    public abstract Throwable getThrown();

    public abstract ThrowableProxy getThrownProxy();

    public abstract boolean isEndOfBatch();

    public abstract boolean isIncludeLocation();

    public abstract void setEndOfBatch(boolean endOfBatch);

    public abstract void setIncludeLocation(boolean locationRequired);

    public abstract long getNanoTime();
}
