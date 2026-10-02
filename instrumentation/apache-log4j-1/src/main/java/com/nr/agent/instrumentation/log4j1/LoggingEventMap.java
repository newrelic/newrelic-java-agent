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

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.INSTRUMENTATION;

class LoggingEventMap {
    static Map<LogAttributeKey, Object> from(LoggingEvent event, boolean appLoggingContextDataEnabled) {
        Map<LogAttributeKey, Object> logEventMap = initialMapWithMdcIfEnabled(event, appLoggingContextDataEnabled);
        logEventMap.put(INSTRUMENTATION, "apache-log4j-1");
        addLoggerInfo(event, logEventMap);
        addMessageAndTs(event, logEventMap);
        addLevel(event, logEventMap);
        addThreadInfo(event, logEventMap);
        addErrorInfo(event, logEventMap);
        return logEventMap;
    }

    private static Map<LogAttributeKey, Object> initialMapWithMdcIfEnabled(LoggingEvent event, boolean isAppLoggingContextDataEnabled) {
        Map<LogAttributeKey, Object> loggingEventMap = null;
        if (isAppLoggingContextDataEnabled) {
            Map<?, ?> mdc = event.getProperties();
            if (mdc != null && !mdc.isEmpty()) {
                loggingEventMap = new HashMap<>(AppLoggingUtils.DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES + mdc.size());
                addMdc(mdc, loggingEventMap);
            }
        }
        if (loggingEventMap == null) {
            loggingEventMap = new HashMap<>(AppLoggingUtils.DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES);
        }
        return loggingEventMap;
    }

    private static void addMdc(Map<?, ?> mdc, Map<LogAttributeKey, Object> map) {
        for (Map.Entry<?, ?> entry : mdc.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            LogAttributeKey logAttrKey = new LogAttributeKey(key.toString(), LogAttributeType.CONTEXT);
            map.put(logAttrKey, value);
        }
    }

    private static void addMessageAndTs(LoggingEvent event, Map<LogAttributeKey, Object> map) {
        String message = event.getRenderedMessage();
        if (message != null && !message.isEmpty()) {
            map.put(AppLoggingUtils.MESSAGE, message);
        }
        map.put(AppLoggingUtils.TIMESTAMP, event.getTimeStamp());
    }

    private static void addErrorInfo(LoggingEvent event, Map<LogAttributeKey, Object> map) {
        Throwable throwable = null;
        ThrowableInformation throwableInformation = event.getThrowableInformation();
        if (throwableInformation != null) {
            throwable = throwableInformation.getThrowable();
        }

        String errorMessage = Log4j1ExceptionUtil.getErrorMessage(throwable);
        if (errorMessage != null) {
            map.put(AppLoggingUtils.ERROR_MESSAGE, errorMessage);
        }

        String errorStack = Log4j1ExceptionUtil.getErrorStack(throwable);
        if (errorStack != null) {
            map.put(AppLoggingUtils.ERROR_STACK, errorStack);
        }

        String errorClass = Log4j1ExceptionUtil.getErrorClass(throwable);
        if (errorClass != null) {
            map.put(AppLoggingUtils.ERROR_CLASS, errorClass);
        }
    }

    private static void addLevel(LoggingEvent event, Map<LogAttributeKey, Object> map) {
        Level level = event.getLevel();
        if (level != null) {
            String levelName = level.toString();
            if (levelName.isEmpty()) {
                map.put(AppLoggingUtils.LEVEL, AppLoggingUtils.UNKNOWN);
            } else {
                map.put(AppLoggingUtils.LEVEL, levelName);
            }
        }
    }

    private static void addThreadInfo(LoggingEvent event, Map<LogAttributeKey, Object> map) {
        String threadName = event.getThreadName();
        if (threadName != null) {
            map.put(AppLoggingUtils.THREAD_NAME, threadName);
        }
        map.put(AppLoggingUtils.THREAD_ID, Thread.currentThread().getId());
    }

    private static void addLoggerInfo(LoggingEvent event, Map<LogAttributeKey, Object> map) {
        String loggerName = event.getLoggerName();
        if (loggerName != null) {
            map.put(AppLoggingUtils.LOGGER_NAME, loggerName);
        }

        String loggerFqcn = event.getFQNOfLoggerClass();
        if (loggerFqcn != null) {
            map.put(AppLoggingUtils.LOGGER_FQCN, loggerFqcn);
        }
    }
}