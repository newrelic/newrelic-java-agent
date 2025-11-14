/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerBuilder;

/**
 * New Relic Java agent implementation of an OpenTelemetry
 * LoggerBuilder, which is a factory for building OpenTelemetry Loggers.
 * An OpenTelemetry Logger can then be used to build and emit OpenTelemetry LogRecords.
 */
class NRLoggerBuilder implements LoggerBuilder {
    private final String instrumentationScopeName;
    private final LoggerSharedState sharedState;
    private String schemaUrl;
    private String instrumentationScopeVersion;

    public NRLoggerBuilder(String instrumentationScopeName, LoggerSharedState sharedState) {
        this.instrumentationScopeName = instrumentationScopeName;
        this.sharedState = sharedState;
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
        return () -> new NRLogRecordBuilder(instrumentationScopeName, instrumentationScopeVersion, schemaUrl, sharedState);
    }
}
