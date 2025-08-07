/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import com.newrelic.api.agent.Config;
import io.opentelemetry.api.OpenTelemetry;
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
    private final Config config;
    private String schemaUrl;
    private String instrumentationScopeVersion;

    public NRLoggerBuilder(Config config, String instrumentationScopeName, LoggerSharedState sharedState) {
        this.config = config;
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
        Boolean enabled = config.getValue("opentelemetry.instrumentation." + instrumentationScopeName + ".enabled");
        if (enabled != null && !enabled) {
            /*
             * This will return a no-op Logger, which results in
             * no OTel LogRecords being emitted, meaning that the
             * customer's logs from OTel APIs will not be written anywhere.
             *
             * If the goal is to prevent NR LogEvents from being created,
             * but continue to emit OTel LogRecords, then that should be
             * done by disabling the logs functionality in agent config:
             *
             *   opentelemetry:
             *    sdk:
             *      logs:
             *        enabled: false
             */
            return OpenTelemetry.noop().getLogsBridge().get(instrumentationScopeName);
        } else {
            return () -> new NRLogRecordBuilder(instrumentationScopeName, instrumentationScopeVersion, schemaUrl, sharedState);
        }
    }
}
