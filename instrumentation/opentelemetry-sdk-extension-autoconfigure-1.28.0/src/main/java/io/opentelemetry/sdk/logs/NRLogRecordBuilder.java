/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import com.nr.agent.instrumentation.utils.logs.LogEventUtil;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * New Relic Java agent implementation of an OpenTelemetry LogRecordBuilder,
 * which is used to build and emit OTel LogRecord instances. In addition to
 * emitting an OpenTelemetry LogRecord, this implementation will create a
 * New Relic LogEvent.
 */
public class NRLogRecordBuilder implements LogRecordBuilder {
    private final Map<String, Object> attributes = new HashMap<>();
    //    private final AttributesMap attributes;
    private final LoggerSharedState loggerSharedState;
    private final InstrumentationScopeInfo instrumentationScopeInfo;
//    private LogLimits logLimits; // TODO: is this needed? Looks like it could be used in setAttribute to configure an AttributesMap

    private long timestampEpochNanos;
    private long observedTimestampEpochNanos;
    private Context context;
    private Severity severity = Severity.UNDEFINED_SEVERITY_NUMBER;
    private String severityText;
    private Body body = Body.empty();

    public NRLogRecordBuilder(String instrumentationScopeName, String instrumentationScopeVersion, String schemaUrl, LoggerSharedState loggerSharedState) {
        this.loggerSharedState = loggerSharedState;

//        this.logLimits = loggerSharedState.getLogLimits();
//        if (this.logLimits == null) {
//            this.logLimits = LogLimits.getDefault();
//        }

        this.instrumentationScopeInfo = InstrumentationScopeInfo
                .builder(instrumentationScopeName)
                .setVersion(instrumentationScopeVersion)
                .setSchemaUrl(schemaUrl)
                .build();

//        this.attributes = initalizeAttributesMap(this.logLimits);
//
//        this.attributes.put(NRLogRecord.OTEL_LIBRARY_NAME, instrumentationScopeName);
//
//        if (instrumentationScopeVersion != null) {
//            this.attributes.put(NRLogRecord.OTEL_LIBRARY_VERSION, instrumentationScopeVersion);
//        }

        attributes.put(NRLogRecord.OTEL_LIBRARY_NAME.getKey(), instrumentationScopeName);

        if (instrumentationScopeVersion != null) {
            attributes.put(NRLogRecord.OTEL_LIBRARY_VERSION.getKey(), instrumentationScopeVersion);
        }
    }

    // Create an AttributesMap with the limits defined in logLimits
//    public AttributesMap initalizeAttributesMap(LogLimits logLimits) {
//        return AttributesMap.create(
//                logLimits.getMaxNumberOfAttributes(),
//                logLimits.getMaxAttributeValueLength()
//        );
//    }

    @Override
    public LogRecordBuilder setTimestamp(long timestamp, TimeUnit unit) {
        this.timestampEpochNanos = unit.toNanos(timestamp);
        return this;
    }

    @Override
    public LogRecordBuilder setTimestamp(Instant instant) {
        this.timestampEpochNanos = TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano();
        return this;
    }

    @Override
    public LogRecordBuilder setObservedTimestamp(long timestamp, TimeUnit unit) {
        this.observedTimestampEpochNanos = unit.toNanos(timestamp);
        return this;
    }

    @Override
    public LogRecordBuilder setObservedTimestamp(Instant instant) {
        this.observedTimestampEpochNanos = TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano();
        return this;
    }

    @Override
    public LogRecordBuilder setContext(Context context) {
        this.context = context;
        return this;
    }

    @Override
    public LogRecordBuilder setSeverity(Severity severity) {
        this.severity = severity;
        return this;
    }

    @Override
    public LogRecordBuilder setSeverityText(String severityText) {
        this.severityText = severityText;
        return this;
    }

    @Override
    public LogRecordBuilder setBody(String body) {
        this.body = Body.string(body);
        return this;
    }

    @Override
    public <T> LogRecordBuilder setAttribute(AttributeKey<T> key, T value) {
        // TODO implement this properly?
        if (key == null || key.getKey().isEmpty() || value == null) {
            return this;
        }
//        if (this.attributes == null) {
//            this.attributes =
//                    AttributesMap.create(
//                            logLimits.getMaxNumberOfAttributes(), logLimits.getMaxAttributeValueLength());
//        }
//        this.attributes.put(key, value);

        this.attributes.put(key.getKey(), value);
        return this;
    }

    /**
     * Intercept here to create a NR LogEvent from the OpenTelemetry
     * LogRecord that is being emitted. The OpenTelemetry LogRecord
     * will still be emitted to its configured destination.
     */
    @Override
    public void emit() {
        if (loggerSharedState.hasBeenShutdown()) {
            return;
        }
        Context context = this.context == null ? Context.current() : this.context;

        long observedTimestampEpochNanos =
                this.observedTimestampEpochNanos == 0
                        ? this.loggerSharedState.getClock().now()
                        : this.observedTimestampEpochNanos;

        NRLogRecord nrLogRecord = NRLogRecord.create(
                loggerSharedState.getLogLimits(),
                loggerSharedState.getResource(),
                instrumentationScopeInfo,
                timestampEpochNanos,
                observedTimestampEpochNanos,
                Span.fromContext(context).getSpanContext(),
                severity,
                severityText,
                body,
//                attributes
                Collections.unmodifiableMap(new HashMap<>(attributes))
        );

        // Pass NRLogRecord through to user configured logRecordProcessor
        // so that the LogRecords get written to the expected destination.
        loggerSharedState.getLogRecordProcessor().onEmit(context, nrLogRecord);

        LogRecordData logRecordData = nrLogRecord.toLogRecordData();

        if (AppLoggingUtils.isApplicationLoggingEnabled()) {
            if (AppLoggingUtils.isApplicationLoggingMetricsEnabled()) {
                // Generate log level metrics
                NewRelic.incrementCounter("Logging/lines");
                NewRelic.incrementCounter("Logging/lines/" + severity.name());
            }

            if (AppLoggingUtils.isApplicationLoggingForwardingEnabled()) {
                // Generate New Relic LogEvents
                LogEventUtil.recordNewRelicLogEvent(logRecordData);

                System.out.println(); // TODO remove logging
                System.out.println(logRecordData); // TODO remove logging
                System.out.println(); // TODO remove logging
            }
        }
    }

    /**
     * Determines if the LogRecordBuilder should be enabled based on the configuration.
     * If auto-configuration is enabled and logs are not explicitly disabled, then
     * the LogRecordBuilder is enabled.
     *
     * @param config the agent configuration
     * @return true if NRLogRecordBuilder should be used, false otherwise
     */
    static boolean isLogRecordBuilderEnabled(Config config) {
        final Boolean autoConfigure = config.getValue("opentelemetry.sdk.autoconfigure.enabled", false);
        if (autoConfigure == null || autoConfigure) {
            // When opentelemetry.sdk.logs.enabled=false, this
            // setting will prevent NR LogEvents from being created,
            // without preventing OTel LogRecords from being emitted.
            final Boolean logsEnabled = config.getValue("opentelemetry.sdk.logs.enabled");
            return logsEnabled == null || logsEnabled;
        }
        return false;
    }
}
