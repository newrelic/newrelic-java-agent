/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.logs;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.logs.NRLogRecord;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_CLASS;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_STACK;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.INSTRUMENTATION;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LEVEL;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_ID;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_NAME;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.TIMESTAMP;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.UNKNOWN;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isAppLoggingContextDataEnabled;
import static io.opentelemetry.sdk.logs.NRLogRecord.BasicLogRecordData;
import static io.opentelemetry.sdk.logs.NRLogRecord.OTEL_EXCEPTION_MESSAGE;
import static io.opentelemetry.sdk.logs.NRLogRecord.OTEL_EXCEPTION_STACKTRACE;
import static io.opentelemetry.sdk.logs.NRLogRecord.OTEL_EXCEPTION_TYPE;
import static io.opentelemetry.sdk.logs.NRLogRecord.OTEL_LIBRARY_NAME;
import static io.opentelemetry.sdk.logs.NRLogRecord.OTEL_LIBRARY_VERSION;
import static io.opentelemetry.sdk.logs.NRLogRecord.OTEL_SCOPE_NAME;
import static io.opentelemetry.sdk.logs.NRLogRecord.OTEL_SCOPE_VERSION;

public class LogEventUtil {
    private static final Set<String> OTEL_ATTRIBUTES = new HashSet<>(Arrays.asList(
            NRLogRecord.OTEL_EXCEPTION_MESSAGE.getKey(),
            NRLogRecord.OTEL_EXCEPTION_TYPE.getKey(),
            NRLogRecord.OTEL_EXCEPTION_STACKTRACE.getKey(),
            NRLogRecord.THREAD_NAME.getKey(),
            NRLogRecord.THREAD_ID_LONG.getKey(),
            NRLogRecord.THREAD_ID_STRING.getKey())
    );

    /**
     * Record a LogEvent to be sent to New Relic.
     *
     * @param logRecordData to parse
     */
    public static void recordNewRelicLogEvent(LogRecordData logRecordData) {
        if (logRecordData != null) {
            Body body = logRecordData.getBody();
            Attributes contextAttributes = logRecordData.getAttributes();
            String errorClass = contextAttributes.get(OTEL_EXCEPTION_TYPE);
            String errorMessage = contextAttributes.get(OTEL_EXCEPTION_MESSAGE);

            if (shouldCreateLogEvent(body, errorClass, errorMessage)) {
                // It is possible that logs are being emitted from OTel instrumentation of a logging framework that we also instrument (e.g. logback, log4j), which could lead to double reporting of LogEvents. We can prevent this by checking if the logs are coming from a known OTel instrumentation source and favoring our own framework instrumentation over it.
                if (LogDuplicationChecker.shouldRecordLogFromOTelAPI()) {
                    Map<LogAttributeKey, Object> logEventMap = new HashMap<>(calculateInitialMapSize(contextAttributes));
                    logEventMap.put(INSTRUMENTATION, "opentelemetry-sdk-extension-autoconfigure-1.28.0");
                    if (body != null && body.getType() == Body.Type.STRING) {
                        String bodyString = body.asString();
                        if (bodyString != null && !bodyString.isEmpty()) {
                            logEventMap.put(MESSAGE, bodyString);
                        }
                    }

                    // Use Timestamp if it is present, otherwise use ObservedTimestamp.
                    long timestampEpochNanos = logRecordData.getTimestampEpochNanos();
                    if (timestampEpochNanos >= 0) {
                        logEventMap.put(TIMESTAMP, timestampEpochNanos);
                    } else {
                        logEventMap.put(TIMESTAMP, logRecordData.getObservedTimestampEpochNanos());
                    }

                    // otel.scope.version and otel.scope.name should be reported along with the deprecated versions otel.library.version and otel.library.name
                    String instrumentationScopeName = logRecordData.getInstrumentationScopeInfo().getName();

                    if (instrumentationScopeName != null && !instrumentationScopeName.isEmpty()) {
                        LogAttributeKey instrumentationScopeNameKey = new LogAttributeKey(OTEL_SCOPE_NAME.getKey(), LogAttributeType.AGENT);
                        logEventMap.put(instrumentationScopeNameKey, instrumentationScopeName);

                        LogAttributeKey instrumentationLibraryNameKey = new LogAttributeKey(OTEL_LIBRARY_NAME.getKey(), LogAttributeType.AGENT);
                        logEventMap.put(instrumentationLibraryNameKey, instrumentationScopeName);
                    }

                    String instrumentationScopeVersion = logRecordData.getInstrumentationScopeInfo().getVersion();

                    if (instrumentationScopeVersion != null && !instrumentationScopeVersion.isEmpty()) {
                        LogAttributeKey instrumentationScopeVersionKey = new LogAttributeKey(OTEL_SCOPE_VERSION.getKey(), LogAttributeType.AGENT);
                        logEventMap.put(instrumentationScopeVersionKey, instrumentationScopeVersion);

                        LogAttributeKey instrumentationLibraryVersionKey = new LogAttributeKey(OTEL_LIBRARY_VERSION.getKey(), LogAttributeType.AGENT);
                        logEventMap.put(instrumentationLibraryVersionKey, instrumentationScopeVersion);
                    }

                    if (isAppLoggingContextDataEnabled()) {
                        for (Map.Entry<AttributeKey<?>, Object> entry : contextAttributes.asMap().entrySet()) {
                            String key = entry.getKey().getKey();
                            // Don't add the context prefix to OTel attributes that are already defined by the OTel spec.
                            if (!OTEL_ATTRIBUTES.contains(key)) {
                                String value = entry.getValue().toString();
                                LogAttributeKey logAttrKey = new LogAttributeKey(key, LogAttributeType.CONTEXT);
                                logEventMap.put(logAttrKey, value);
                            }
                        }
                    }

                    // These attributes come from the attribute map, but they should not be prefixed with context since they are defined in the OTel semantic conventions.
                    if (!contextAttributes.isEmpty()) {
                        // Exceptions are captured in the attributes map for OTel logs.
                        // https://opentelemetry.io/docs/specs/semconv/exceptions/exceptions-logs/
                        if (ExceptionUtil.getErrorMessage(errorMessage) != null) {
                            logEventMap.put(ERROR_MESSAGE, errorMessage);
                        }

                        if (ExceptionUtil.getErrorClass(errorClass) != null) {
                            logEventMap.put(ERROR_CLASS, errorClass);
                        }

                        String errorStack = ExceptionUtil.getErrorStack(contextAttributes.get(OTEL_EXCEPTION_STACKTRACE));
                        if (errorStack != null) {
                            logEventMap.put(ERROR_STACK, errorStack);
                        }
                    }

                    Severity severity = logRecordData.getSeverity();
                    if (severity != null) {
                        // Use SeverityText if it is present, otherwise use SeverityNumber and convert it to a textual representation based on the enum value.
                        String severityName = severity.toString();
                        if (severityName == null || severityName.isEmpty()) {
                            int severityNumber = severity.getSeverityNumber();
                            Severity[] severityValues = Severity.values();
                            Severity severityValue = severityValues[severityNumber];
                            severityName = severityValue.toString();
                            // If we still don't have a valid severity name, set it to "UNKNOWN"
                            if (severityName == null || severityName.isEmpty()) {
                                logEventMap.put(LEVEL, UNKNOWN);
                            } else {
                                logEventMap.put(LEVEL, severityName);
                            }
                        } else {
                            logEventMap.put(LEVEL, severityName);
                        }
                    }

                    String threadName = ((BasicLogRecordData) logRecordData).getThreadName();
                    if (threadName != null) {
                        logEventMap.put(THREAD_NAME, threadName);
                    }

                    long threadId = ((BasicLogRecordData) logRecordData).getThreadId();
                    logEventMap.put(THREAD_ID, threadId);

                    AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
                }
            }
        }
    }

    /**
     * A LogEvent should be created if a log message or an error is logged.
     *
     * @param body         Message to validate
     * @param errorClass   String to validate from OTel exception.type
     * @param errorMessage String to validate from OTel exception.message
     * @return true if a LogEvent should be created, otherwise false
     */
    private static boolean shouldCreateLogEvent(Body body, String errorClass, String errorMessage) {
        return (body != null) || (ExceptionUtil.getErrorClass(errorClass) != null) || (ExceptionUtil.getErrorMessage(errorMessage) != null);
    }

    private static int calculateInitialMapSize(Attributes attributes) {
        return isAppLoggingContextDataEnabled() && attributes != null
                ? attributes.size() + DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES
                : DEFAULT_NUM_OF_LOG_EVENT_ATTRIBUTES;
    }
}
