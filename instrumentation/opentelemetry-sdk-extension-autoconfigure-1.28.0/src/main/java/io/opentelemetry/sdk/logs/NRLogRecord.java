/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import com.nr.agent.instrumentation.utils.AttributesHelper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;

import java.util.Map;

/**
 * New Relic Java representation of an OpenTelemetry LogRecord.
 */
public class NRLogRecord implements ReadWriteLogRecord {
    public static final AttributeKey<String> OTEL_SCOPE_VERSION = AttributeKey.stringKey("otel.scope.version");
    public static final AttributeKey<String> OTEL_SCOPE_NAME = AttributeKey.stringKey("otel.scope.name");
    public static final AttributeKey<String> OTEL_LIBRARY_VERSION = AttributeKey.stringKey("otel.library.version");
    public static final AttributeKey<String> OTEL_LIBRARY_NAME = AttributeKey.stringKey("otel.library.name");
    public static final AttributeKey<String> OTEL_EXCEPTION_MESSAGE = AttributeKey.stringKey("exception.message");
    public static final AttributeKey<String> OTEL_EXCEPTION_TYPE = AttributeKey.stringKey("exception.type");
    public static final AttributeKey<String> OTEL_EXCEPTION_STACKTRACE = AttributeKey.stringKey("exception.stacktrace");

    private final LogLimits logLimits; // LogLimits is not used in this implementation, but kept for compatibility
    private final Resource resource;
    private final InstrumentationScopeInfo instrumentationScopeInfo;
    private final long timestampEpochNanos;
    private final long observedTimestampEpochNanos;
    private final SpanContext spanContext;
    private final Severity severity;
    private final String severityText;
    private final Body body;
    private final Object lock = new Object();
    private final Map<String, Object> attributes;

    private NRLogRecord(
            LogLimits logLimits,
            Resource resource,
            InstrumentationScopeInfo instrumentationScopeInfo,
            long timestampEpochNanos,
            long observedTimestampEpochNanos,
            SpanContext spanContext,
            Severity severity,
            String severityText,
            Body body,
            Map<String, Object> attributes
    ) {
        this.logLimits = logLimits;
        this.resource = resource;
        this.instrumentationScopeInfo = instrumentationScopeInfo;
        this.timestampEpochNanos = timestampEpochNanos;
        this.observedTimestampEpochNanos = observedTimestampEpochNanos;
        this.spanContext = spanContext;
        this.severity = severity;
        this.severityText = severityText;
        this.body = body;
        this.attributes = attributes;
    }

    /**
     * Create the OTel LogRecord.
     */
    static NRLogRecord create(
            LogLimits logLimits,
            Resource resource,
            InstrumentationScopeInfo instrumentationScopeInfo,
            long timestampEpochNanos,
            long observedTimestampEpochNanos,
            SpanContext spanContext,
            Severity severity,
            String severityText,
            Body body,
            Map<String, Object> attributes) {
        return new NRLogRecord(
                logLimits,
                resource,
                instrumentationScopeInfo,
                timestampEpochNanos,
                observedTimestampEpochNanos,
                spanContext,
                severity,
                severityText,
                body,
                attributes);
    }

    @Override
    public <T> ReadWriteLogRecord setAttribute(AttributeKey<T> key, T value) {
        if (key == null || key.getKey().isEmpty() || value == null) {
            return this;
        }
        synchronized (lock) {
            attributes.put(key.getKey(), value);
        }
        return this;
    }

    /**
     * @return an immutable LogRecordData instance representing this log record.
     */
    @Override
    public LogRecordData toLogRecordData() {
        synchronized (lock) {
            return BasicLogRecordData.create(
                    resource,
                    instrumentationScopeInfo,
                    timestampEpochNanos,
                    observedTimestampEpochNanos,
                    spanContext,
                    severity,
                    severityText,
                    body,
                    AttributesHelper.toAttributes(attributes),
                    attributes.size()
            );
        }
    }

    public static class BasicLogRecordData implements LogRecordData {
        private final Resource resource;
        private final InstrumentationScopeInfo instrumentationScopeInfo;
        private final long timestampEpochNanos;
        private final long observedTimestampEpochNanos;
        private final SpanContext spanContext;
        private final Severity severity;
        private final String severityText;
        private final Body body;
        private final Attributes attributes;
        private final int totalAttributeCount;

        private final long threadId;
        private final String threadName;

        private BasicLogRecordData(
                Resource resource,
                InstrumentationScopeInfo instrumentationScopeInfo,
                long timestampEpochNanos,
                long observedTimestampEpochNanos,
                SpanContext spanContext,
                Severity severity,
                String severityText,
                Body body,
                Attributes attributes,
                int totalAttributeCount) {
            this.resource = resource;
            this.instrumentationScopeInfo = instrumentationScopeInfo;
            this.timestampEpochNanos = timestampEpochNanos;
            this.observedTimestampEpochNanos = observedTimestampEpochNanos;
            this.spanContext = spanContext;
            this.severity = severity;
            this.severityText = severityText;
            this.body = body;
            this.attributes = attributes;
            this.totalAttributeCount = totalAttributeCount;
            // Capture thread information at the time of log record creation, OTel does not provide this
            this.threadId = Thread.currentThread().getId();
            this.threadName = Thread.currentThread().getName();
        }

        static BasicLogRecordData create(
                Resource resource,
                InstrumentationScopeInfo instrumentationScopeInfo,
                long timestampEpochNanos,
                long observedTimestampEpochNanos,
                SpanContext spanContext,
                Severity severity,
                String severityText,
                Body body,
                Attributes attributes,
                int totalAttributeCount) {
            return new BasicLogRecordData(
                    resource,
                    instrumentationScopeInfo,
                    timestampEpochNanos,
                    observedTimestampEpochNanos,
                    spanContext,
                    severity,
                    severityText,
                    body,
                    attributes,
                    totalAttributeCount
            );
        }

        public long getThreadId() {
            return threadId;
        }

        public String getThreadName() {
            return threadName;
        }

        @Override
        public Resource getResource() {
            return resource;
        }

        @Override
        public InstrumentationScopeInfo getInstrumentationScopeInfo() {
            return instrumentationScopeInfo;
        }

        @Override
        public long getTimestampEpochNanos() {
            return timestampEpochNanos;
        }

        @Override
        public long getObservedTimestampEpochNanos() {
            return observedTimestampEpochNanos;
        }

        @Override
        public SpanContext getSpanContext() {
            return spanContext;
        }

        @Override
        public Severity getSeverity() {
            return severity;
        }

        @Override
        public String getSeverityText() {
            return severityText;
        }

        @Override
        public Body getBody() {
            return body;
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public int getTotalAttributeCount() {
            return totalAttributeCount;
        }

        @Override
        public String toString() {
            return "BasicLogRecordData{resource="
                    + this.resource
                    + ", instrumentationScopeInfo="
                    + this.instrumentationScopeInfo
                    + ", timestampEpochNanos="
                    + this.timestampEpochNanos
                    + ", observedTimestampEpochNanos="
                    + this.observedTimestampEpochNanos
                    + ", spanContext="
                    + this.spanContext
                    + ", severity="
                    + this.severity
                    + ", severityText="
                    + this.severityText
                    + ", body="
                    + this.body
                    + ", attributes="
                    + this.attributes
                    + ", totalAttributeCount="
                    + this.totalAttributeCount
                    + ", threadId="
                    + this.threadId
                    + ", threadName="
                    + this.threadName
                    + "}";
        }
    }
}
