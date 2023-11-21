/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.opentelemetry;

import com.newrelic.api.agent.TraceMetadata;

final class NoOpTraceMetadata implements TraceMetadata {

    private static final NoOpTraceMetadata INSTANCE = new NoOpTraceMetadata();

    NoOpTraceMetadata() {
    }

    static NoOpTraceMetadata getInstance() {
        return INSTANCE;
    }

    @Override
    public String getTraceId() {
        OpenTelemetryNewRelic.logUnsupportedMethod("TraceMetadata", "getTraceId");
        return "";
    }

    @Override
    public String getSpanId() {
        OpenTelemetryNewRelic.logUnsupportedMethod("TraceMetadata", "getSpanId");
        return "";
    }

    @Override
    public boolean isSampled() {
        OpenTelemetryNewRelic.logUnsupportedMethod("TraceMetadata", "isSampled");
        return false;
    }
}
