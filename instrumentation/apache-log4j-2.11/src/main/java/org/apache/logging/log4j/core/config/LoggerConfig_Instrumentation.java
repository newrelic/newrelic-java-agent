/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.logging.log4j.core.config;

import com.newrelic.agent.bridge.logging.LinkingMetadataHolder;
import com.newrelic.agent.bridge.logging.Log4jUtils;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LogEvent_Instrumentation;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingForwardingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingMetricsEnabled;
import static com.nr.agent.instrumentation.log4j2.AgentUtil.recordNewRelicLogEvent;

@Weave(originalName = "org.apache.logging.log4j.core.config.LoggerConfig", type = MatchType.ExactClass)
public class LoggerConfig_Instrumentation {
    @NewField
    public static AtomicBoolean instrumented = new AtomicBoolean(false);

    @WeaveAllConstructors
    public LoggerConfig_Instrumentation() {
        // Generate the instrumentation module supportability metric only once
        if (!instrumented.getAndSet(true)) {
            NewRelic.incrementCounter("Supportability/Logging/Java/Log4j2.11/enabled");
        }
    }

    protected void callAppenders(LogEvent_Instrumentation event) {
        LinkingMetadataHolder agentLinkingMetadata = Log4jUtils.getLinkingMetadataFromCache(event);
        NewRelic.getAgent().getLogger().log(Level.INFO, "callappend: {0}", event);
        NewRelic.getAgent().getLogger().log(Level.INFO, "callappend: {0}", agentLinkingMetadata);
        NewRelic.getAgent().getLogger().log(Level.INFO, "callappend: {0}", NewRelic.getAgent().getTransaction());

        // Do nothing if application_logging.enabled: false
        Map<String, String> aqentLinkingMetadata = null;
        if (isApplicationLoggingEnabled()) {
            // Do nothing if logger has parents and isAdditive is set to true to avoid duplicated counters and logs
            if (getParent() == null || !isAdditive()) {
                if (isApplicationLoggingMetricsEnabled()) {
                    // Generate log level metrics
                    NewRelic.incrementCounter("Logging/lines");
                    NewRelic.incrementCounter("Logging/lines/" + event.getLevel().toString());
                }

                if (isApplicationLoggingForwardingEnabled()) {
                    // Record and send LogEvent to New Relic
                    agentLinkingMetadata = Log4jUtils.getLinkingMetadataFromCache(event);
                    recordNewRelicLogEvent((LogEvent) event, agentLinkingMetadata);
                }
            }
        }

        Weaver.callOriginal();

        if (agentLinkingMetadata.isValid()) {
            Log4jUtils.removeLinkingMetadataFromCache(event);
        }
    }

    public LoggerConfig getParent() {
        return Weaver.callOriginal();
    }

    public boolean isAdditive() {
        return Weaver.callOriginal();
    }

}
