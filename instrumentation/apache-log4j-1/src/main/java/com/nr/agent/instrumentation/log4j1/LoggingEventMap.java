/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.log4j1;

import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.HashMap;
import java.util.Map;

<<<<<<< HEAD
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_CLASS;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_STACK;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LEVEL;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LOGGER_FQCN;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LOGGER_NAME;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_ID;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_NAME;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.TIMESTAMP;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.UNKNOWN;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.INSTRUMENTATION;

class LoggingEventMap {

    static Map<LogAttributeKey, Object> from(LoggingEvent event, boolean appLoggingContextDataEnabled, Map<String, String> tags) {
        Map<LogAttributeKey, Object> logEventMap = initialMapWithMdcIfEnabled(event, appLoggingContextDataEnabled, tags);
        logEventMap.put(INSTRUMENTATION, "apache-log4j-1");
        addTags(tags, logEventMap);
        addLoggerInfo(event, logEventMap);
        addMessageAndTimestamp(event, logEventMap);
        addLevel(event, logEventMap);
        addThreadInfo(event, logEventMap);
        addErrorInfo(event, logEventMap);
        return logEventMap;
    }

    private static Map<LogAttributeKey, Object> initialMapWithMdcIfEnabled(LoggingEvent event, boolean isAppLoggingContextDataEnabled, Map<String, String> tags) {
        Map<LogAttributeKey, Object> loggingEventMap = null;
        int potentialMapEntries = DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES + tags.size();
        if (isAppLoggingContextDataEnabled) {
            Map<?, ?> mdc = event.getProperties();
            if (mdc != null && !mdc.isEmpty()) {
                loggingEventMap = new HashMap<>(potentialMapEntries + mdc.size());
                addMdc(mdc, loggingEventMap);
            }
        }
        if (loggingEventMap == null) {
            loggingEventMap = new HashMap<>(potentialMapEntries);
        }
        return loggingEventMap;
    }

    private static void addMdc(Map<?, ?> mdc, Map<LogAttributeKey, Object> logEventMap) {
        mdc.forEach((key, value) -> {
            LogAttributeKey logAttrKey = new LogAttributeKey(key.toString(), LogAttributeType.CONTEXT);
            logEventMap.put(logAttrKey, value);
        });
    }

    private static void addMessageAndTimestamp(LoggingEvent event, Map<LogAttributeKey, Object> logEventMap) {
        String message = event.getRenderedMessage();
        if (message != null && !message.isEmpty()) {
            logEventMap.put(MESSAGE, message);
        }
        logEventMap.put(TIMESTAMP, event.getTimeStamp());
    }

    private static void addErrorInfo(LoggingEvent event, Map<LogAttributeKey, Object> logEventMap) {
        Throwable thrown = null;
        ThrowableInformation throwableInformation = event.getThrowableInformation();
        if (throwableInformation != null) {
            thrown = throwableInformation.getThrowable();
        }

        String errorMessage = Log4j1ExceptionUtil.getErrorMessage(thrown);
        if (errorMessage != null) {
            logEventMap.put(ERROR_MESSAGE, errorMessage);
        }

        String errorStack = Log4j1ExceptionUtil.getErrorStack(thrown);
        if (errorStack != null) {
            logEventMap.put(ERROR_STACK, errorStack);
        }

        String errorClass = Log4j1ExceptionUtil.getErrorClass(thrown);
        if (errorClass != null) {
            logEventMap.put(ERROR_CLASS, errorClass);
        }
    }

    private static void addLevel(LoggingEvent event, Map<LogAttributeKey, Object> logEventMap) {
        Level level = event.getLevel();
        if (level != null) {
            String levelName = level.toString();
            if (levelName.isEmpty()) {
                logEventMap.put(LEVEL, UNKNOWN);
            } else {
                logEventMap.put(LEVEL, levelName);
            }
        }
    }

    private static void addThreadInfo(LoggingEvent event, Map<LogAttributeKey, Object> logEventMap) {
        String threadName = event.getThreadName();
        if (threadName != null) {
            logEventMap.put(THREAD_NAME, threadName);
        }
        logEventMap.put(THREAD_ID, Thread.currentThread().getId());
    }

    private static void addLoggerInfo(LoggingEvent event, Map<LogAttributeKey, Object> logEventMap) {
        String loggerName = event.getLoggerName();
        if (loggerName != null) {
            logEventMap.put(LOGGER_NAME, loggerName);
        }

        String loggerFqcn = event.getFQNOfLoggerClass();
        if (loggerFqcn != null) {
            logEventMap.put(LOGGER_FQCN, loggerFqcn);
        }
    }

    private static void addTags(Map<String, String> tags, Map<LogAttributeKey, Object> logEventMap) {
        tags.forEach((key, value) -> {
            LogAttributeKey logAttrKey = new LogAttributeKey(key, LogAttributeType.TAG);
            logEventMap.put(logAttrKey, value);
        });
    }
}