/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerBuilder;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;

import java.util.function.Supplier;

public class TestLoggerBuilder implements LoggerBuilder {
    private final String instrumentationScopeName;
    private String instrumentationScopeVersion;
    private LogRecordProcessor logRecordProcessor;
    private Resource resource = Resource.empty();
    private String schemaUrl;

    public TestLoggerBuilder(String instrumentationScopeName) {
        this.instrumentationScopeName = instrumentationScopeName;
    }

    public TestLoggerBuilder addLogRecordProcessor(LogRecordProcessor processor) {
        this.logRecordProcessor = processor;
        return this;
    }

    public TestLoggerBuilder setResource(Resource resource) {
        this.resource = resource;
        return this;
    }

    @Override
    public LoggerBuilder setSchemaUrl(String schemaUrl) {
        this.schemaUrl = schemaUrl;
        return this;
    }

    @Override
    public LoggerBuilder setInstrumentationVersion(String instrumentationScopeVersion) {
        this.instrumentationScopeVersion = instrumentationScopeVersion;
        return this;
    }

    @Override
    public Logger build() {
        Supplier<LogLimits> logLimitsSupplier = () -> LogLimits.getDefault();
        LoggerSharedState sharedState = new LoggerSharedState(resource, logLimitsSupplier, logRecordProcessor, Clock.getDefault());
        return () -> new NRLogRecordBuilder(instrumentationScopeName, instrumentationScopeVersion, schemaUrl, sharedState);
    }
}
