/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.log4j1;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.api.agent.NewRelic;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.*;

public class Log4j1Util {
    // Keep track of first time a log4j1 configuration is loaded and instrumented
    public static AtomicBoolean log4j1Instrumented = new AtomicBoolean(false);

    public static void setLog4j1Enabled() {
        if (!log4j1Instrumented.getAndSet(true)) {
            NewRelic.incrementCounter("Supportability/Logging/Java/Log4j1/enabled");
        }
    }

    public static String appendAgentMetadataIfLocalDecoratingEnabled(String formattedLogMessage) {
        if (formattedLogMessage != null && isApplicationLoggingLocalDecoratingEnabled()) {
            int breakLine = formattedLogMessage.lastIndexOf("\n");
            StringBuilder builder = new StringBuilder(formattedLogMessage);
            if (breakLine != -1) {
                builder.replace(breakLine, breakLine + 1, "");
            }
            return builder.append(getLinkingMetadataBlob()).append("\n").toString();
        }
        return formattedLogMessage;
    }

    public static void generateMetricsAndOrLogEventIfEnabled(LoggingEvent event) {
        if (isApplicationLoggingEnabled()) {
            if (isApplicationLoggingMetricsEnabled()) {
                generateLogMetrics(event);
            }
            if (isApplicationLoggingForwardingEnabled()) {
                recordNewRelicLogEvent(event);
            }
        }
    }

    private static void generateLogMetrics(LoggingEvent event) {
        NewRelic.incrementCounter("Logging/lines");
        NewRelic.incrementCounter("Logging/lines/" + event.getLevel().toString());
    }

    private static void recordNewRelicLogEvent(LoggingEvent event) {
        if (shouldCreateNewRelicLogEventFor(event)) {
            boolean isAppLoggingContextDataEnabled = AppLoggingUtils.isAppLoggingContextDataEnabled();
            Map<String, String> tags = AppLoggingUtils.getTags();
            Map<LogAttributeKey, Object> logEventMap = LoggingEventMap.from(event, isAppLoggingContextDataEnabled, tags);
            AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
        }
    }

    private static boolean shouldCreateNewRelicLogEventFor(LoggingEvent event) {
        return event != null &&
                (event.getMessage() != null || hasThrowable(event));
    }

    private static boolean hasThrowable(LoggingEvent event) {
        return event.getThrowableInformation() != null &&
                event.getThrowableInformation().getThrowable() != null;
    }
}
