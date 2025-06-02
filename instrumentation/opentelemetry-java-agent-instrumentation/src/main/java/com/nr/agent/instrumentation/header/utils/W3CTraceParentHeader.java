/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.header.utils;

import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.SpanContext;

public class W3CTraceParentHeader {

    static final String W3C_VERSION = "00";
    static final String W3C_TRACE_PARENT_DELIMITER = "-";

    public static String create(SpanContext parentSpanContext) {
        final String traceId = parentSpanContext.getTraceId();
        final String spanId = parentSpanContext.getSpanId();
        final boolean sampled = parentSpanContext.isSampled();

        String traceParentHeader =
                W3C_VERSION + W3C_TRACE_PARENT_DELIMITER + traceId + W3C_TRACE_PARENT_DELIMITER + spanId + W3C_TRACE_PARENT_DELIMITER + sampledToFlags(sampled);

        boolean valid = W3CTraceParentValidator.forHeader(traceParentHeader)
                .version(W3C_VERSION)
                .traceId(traceId)
                .parentId(spanId)
                .flags(parentSpanContext.getTraceFlags().asHex())
                .isValid();

        return valid ? traceParentHeader : "";
    }

    private static String sampledToFlags(boolean sampled) {
        return sampled ? "01" : "00";
    }
}
