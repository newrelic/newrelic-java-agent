/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.log4j2;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.bridge.logging.LinkingMetadataHolder;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_CLASS;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_STACK;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.INSTRUMENTATION;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LEVEL;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LOGGER_FQCN;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LOGGER_NAME;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_ID;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_NAME;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.TIMESTAMP;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.UNKNOWN;

public class AgentUtil {
    /**
     * Record a LogEvent to be sent to New Relic.
     *
     * @param event to parse
     */
    public static void recordNewRelicLogEvent(LogEvent event, LinkingMetadataHolder linkingMetadata) {
        if (event != null) {
            Message message = event.getMessage();
            Throwable throwable = event.getThrown();

            if (shouldCreateLogEvent(message, throwable)) {
                ReadOnlyStringMap contextData = event.getContextData();
                Map<LogAttributeKey, Object> logEventMap = new HashMap<>(calculateInitialMapSize(contextData));
                logEventMap.put(INSTRUMENTATION, "apache-log4j-2.11");
                if (message != null) {
                    String formattedMessage = message.getFormattedMessage();
                    if (formattedMessage != null && !formattedMessage.isEmpty()) {
                        logEventMap.put(MESSAGE, formattedMessage);
                    }
                }
                logEventMap.put(TIMESTAMP, event.getTimeMillis());

                if (AppLoggingUtils.isAppLoggingContextDataEnabled() && contextData != null) {
                    for (Map.Entry<String, String> entry : contextData.toMap().entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        LogAttributeKey logAttrKey = new LogAttributeKey(key, LogAttributeType.CONTEXT);
                        logEventMap.put(logAttrKey, value);
                    }
                }

                Level level = event.getLevel();
                if (level != null) {
                    String levelName = level.name();
                    if (levelName.isEmpty()) {
                        logEventMap.put(LEVEL, UNKNOWN);
                    } else {
                        logEventMap.put(LEVEL, levelName);
                    }
                }

                String errorStack = ExceptionUtil.getErrorStack(throwable);
                if (errorStack != null) {
                    logEventMap.put(ERROR_STACK, errorStack);
                }

                String errorMessage = ExceptionUtil.getErrorMessage(throwable);
                if (errorMessage != null) {
                    logEventMap.put(ERROR_MESSAGE, errorMessage);
                }

                String errorClass = ExceptionUtil.getErrorClass(throwable);
                if (errorClass != null) {
                    logEventMap.put(ERROR_CLASS, errorClass);
                }

                String threadName = event.getThreadName();
                if (threadName != null) {
                    logEventMap.put(THREAD_NAME, threadName);
                }

                logEventMap.put(THREAD_ID, event.getThreadId());

                String loggerName = event.getLoggerName();
                if (loggerName != null) {
                    logEventMap.put(LOGGER_NAME, loggerName);
                }

                String loggerFqcn = event.getLoggerFqcn();
                if (loggerFqcn != null) {
                    logEventMap.put(LOGGER_FQCN, loggerFqcn);
                }

                if (linkingMetadata != null) {
                    AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap, linkingMetadata);
                } else {
                    AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
                }
            }
        }
    }

    /**
     * A LogEvent MUST NOT be reported if neither a log message nor an error is logged. If either is present report the LogEvent.
     *
     * @param message   Message to validate
     * @param throwable Throwable to validate
     * @return true if a LogEvent should be created, otherwise false
     */
    private static boolean shouldCreateLogEvent(Message message, Throwable throwable) {
        return (message != null) || !ExceptionUtil.isThrowableNull(throwable);
    }

    private static int calculateInitialMapSize(ReadOnlyStringMap mdcPropertyMap) {
        return AppLoggingUtils.isAppLoggingContextDataEnabled() && mdcPropertyMap != null
                ? mdcPropertyMap.size() + DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES
                : DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES;
    }

    /**
     * Checks pretty or compact JSON layout strings for a series of characters and returns the index of
     * the characters or -1 if they were not found. This is used to find the log "message" substring
     * so that the NR-LINKING metadata blob can be inserted when using local decorating with JsonLayout.
     *
     * @param writerString String representing JSON formatted log event
     * @return positive int if index was found, else -1
     */
    public static int getIndexToModifyJson(String writerString) {
        return writerString.indexOf("\",", writerString.indexOf("message"));
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

}
