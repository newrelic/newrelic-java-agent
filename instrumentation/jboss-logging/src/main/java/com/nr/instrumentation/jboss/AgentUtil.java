/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jboss;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import org.jboss.logmanager.ExtLogRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

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
     * @param record to parse
     */
    public static void recordNewRelicLogEvent(ExtLogRecord record) {
        if (record != null) {
            String message = record.getMessage();
            Throwable thrown = record.getThrown();

            if (shouldCreateLogEvent(message, thrown)) {
                Map<String, String> mdcCopy = record.getMdcCopy();
                Map<LogAttributeKey, Object> logEventMap = new HashMap<>(calculateInitialMapSize(mdcCopy));
                logEventMap.put(INSTRUMENTATION, "jboss.logging");
                logEventMap.put(MESSAGE, message);
                logEventMap.put(TIMESTAMP, record.getMillis());

                if (AppLoggingUtils.isAppLoggingContextDataEnabled() && mdcCopy != null) {
                    addMdc(mdcCopy, logEventMap);
                }
                addTags(logEventMap);
                Level level = record.getLevel();
                addLevel(level, logEventMap);
                addErrorInfo(thrown, logEventMap);

                String threadName = Thread.currentThread().getName();
                if (threadName != null) {
                    logEventMap.put(THREAD_NAME, threadName);
                }

                logEventMap.put(THREAD_ID, record.getThreadID());

                String loggerName = record.getLoggerName();
                if (loggerName != null) {
                    logEventMap.put(LOGGER_NAME, loggerName);
                }

                String loggerFqcn = record.getSourceClassName();
                if (loggerFqcn != null) {
                    logEventMap.put(LOGGER_FQCN, loggerFqcn);
                }

                AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
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
    private static boolean shouldCreateLogEvent(String message, Throwable throwable) {
        return (message != null) || !ExceptionUtil.isThrowableNull(throwable);
    }

    private static int calculateInitialMapSize(Map<String, String> mdcPropertyMap) {
        return (AppLoggingUtils.isAppLoggingContextDataEnabled() && mdcPropertyMap != null
                ? mdcPropertyMap.size() + DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES
                : DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES)
                + AppLoggingUtils.getTags().size();
    }

    private static void addMdc(Map<String, String> mdcPropertyMap, Map<LogAttributeKey, Object> logEventMap) {
        if (AppLoggingUtils.isAppLoggingContextDataEnabled()) {
            mdcPropertyMap.forEach((key, value) -> {
                LogAttributeKey logAttrKey = new LogAttributeKey(key, LogAttributeType.CONTEXT);
                logEventMap.put(logAttrKey, value);
            });
        }
    }

    private static void addTags(Map<LogAttributeKey, Object> logEventMap) {
        AppLoggingUtils.getTags().forEach((key, value) -> {
            LogAttributeKey logAttrKey = new LogAttributeKey(key, LogAttributeType.TAG);
            logEventMap.put(logAttrKey, value);
        });
    }

    private static void addLevel(Level level, Map<LogAttributeKey, Object> logEventMap) {
        String levelName = level.getName();
        if (levelName.isEmpty()) {
            logEventMap.put(LEVEL, UNKNOWN);
        } else {
            logEventMap.put(LEVEL, levelName);
        }
    }

    private static void addErrorInfo(Throwable thrown, Map<LogAttributeKey, Object> logEventMap) {
        String errorStack = ExceptionUtil.getErrorStack(thrown);
        if (errorStack != null) {
            logEventMap.put(ERROR_STACK, errorStack);
        }

        String errorMessage = ExceptionUtil.getErrorMessage(thrown);
        if (errorMessage != null) {
            logEventMap.put(ERROR_MESSAGE, errorMessage);
        }

        String errorClass = ExceptionUtil.getErrorClass(thrown);
        if (errorClass != null) {
            logEventMap.put(ERROR_CLASS, errorClass);
        }
    }
}