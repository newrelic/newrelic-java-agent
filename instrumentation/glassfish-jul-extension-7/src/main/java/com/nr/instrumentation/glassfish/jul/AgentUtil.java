/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.glassfish.jul;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.LogAttributeKey;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

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
    public static void recordNewRelicLogEvent(LogRecord record) {
        if (record != null) {
            String message = record.getMessage();
            Throwable throwable = record.getThrown();

            if (shouldCreateLogEvent(message, throwable)) {
                // JUL does not directly support MDC, so we only initialize the map size based on standard attributes
                Map<LogAttributeKey, Object> logEventMap = new HashMap<>(DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES);
                logEventMap.put(INSTRUMENTATION, "glassfish-jul-extension-7");
                logEventMap.put(MESSAGE, message);
                logEventMap.put(TIMESTAMP, record.getMillis());

                Level level = record.getLevel();
                if (level != null) {
                    String levelName = level.getName();
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
}
