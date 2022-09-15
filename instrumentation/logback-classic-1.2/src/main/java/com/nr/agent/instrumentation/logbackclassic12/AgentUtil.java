/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.logbackclassic12;

import ch.qos.logback.classic.Level;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;

import java.util.HashMap;
import java.util.Map;

public class AgentUtil {
    public static final int DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES = 10;
    // Log message attributes
    /**
     * Record a LogEvent to be sent to New Relic.
     *
     * @param message         log message
     * @param timeStampMillis log timestamp
     * @param level           log level
     */
    public static void recordNewRelicLogEvent(String message, Map<String, String> mdcPropertyMap, long timeStampMillis, Level level, Throwable throwable, String threadName, long threadId,
            String loggerName, String fqcnLoggerName) {
        boolean messageEmpty = message.isEmpty();

        if (shouldCreateLogEvent(messageEmpty, throwable)) {
            Map<LogAttributeKey, Object> logEventMap = new HashMap<>(DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES);

            if (!messageEmpty) {
                logEventMap.put(AppLoggingUtils.MESSAGE, message);
            }
            logEventMap.put(AppLoggingUtils.TIMESTAMP, timeStampMillis);

            if (AppLoggingUtils.isAppLoggingContextDataEnabled()) {
                for (Map.Entry<String, String> mdcEntry : mdcPropertyMap.entrySet()) {
                    LogAttributeKey logAttrKey = new LogAttributeKey(mdcEntry.getKey(), LogAttributeType.CONTEXT);
                    logEventMap.put(logAttrKey, mdcEntry.getValue());
                }
            }

            if (level.toString().isEmpty()) {
                logEventMap.put(AppLoggingUtils.LEVEL, AppLoggingUtils.UNKNOWN);
            } else {
                logEventMap.put(AppLoggingUtils.LEVEL, level);
            }

            String errorStack = ExceptionUtil.getErrorStack(throwable);
            if (errorStack != null) {
                logEventMap.put(AppLoggingUtils.ERROR_STACK, errorStack);
            }

            String errorMessage = ExceptionUtil.getErrorMessage(throwable);
            if (errorMessage != null) {
                logEventMap.put(AppLoggingUtils.ERROR_MESSAGE, errorMessage);
            }

            String errorClass = ExceptionUtil.getErrorClass(throwable);
            if (errorClass != null) {
                logEventMap.put(AppLoggingUtils.ERROR_CLASS, errorClass);
            }

            if (threadName != null) {
                logEventMap.put(AppLoggingUtils.THREAD_NAME, threadName);
            }

            logEventMap.put(AppLoggingUtils.THREAD_ID, threadId);

            if (loggerName != null) {
                logEventMap.put(AppLoggingUtils.LOGGER_NAME, loggerName);
            }

            if (fqcnLoggerName != null) {
                logEventMap.put(AppLoggingUtils.LOGGER_FQCN, fqcnLoggerName);
            }

            AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
        }
    }

    /**
     * A LogEvent MUST NOT be reported if neither a log message nor an error is logged. If either is present report the LogEvent.
     *
     * @param messageEmpty Message to validate
     * @param throwable    Throwable to validate
     * @return true if a LogEvent should be created, otherwise false
     */
    private static boolean shouldCreateLogEvent(boolean messageEmpty, Throwable throwable) {
        return !messageEmpty || !ExceptionUtil.isThrowableNull(throwable);
    }

    private static int getDefaultNumOfLogEventAttributes(Map<String, String> mdcPropertyMap) {
        return AppLoggingUtils.isAppLoggingContextDataEnabled()
                ? mdcPropertyMap.size() + DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES
                : DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES;
    }
}
