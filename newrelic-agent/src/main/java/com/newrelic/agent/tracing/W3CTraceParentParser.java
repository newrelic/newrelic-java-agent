/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.service.ServiceFactory;

import java.util.List;
import java.util.logging.Level;

public class W3CTraceParentParser {

    static W3CTraceParent parseHeaders(List<String> traceParentHeaders) {
        if (traceParentHeaders.size() != 1) {
            ServiceFactory.getStatsService().getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_PARENT_HEADER_COUNT);
            Agent.LOG.log(Level.WARNING, "Multiple traceparent headers found on inbound request.");
            // Multiple values ok if all are equal
            boolean allHeadersEqual = traceParentHeaders.stream().allMatch(h -> h.equals(traceParentHeaders.get(0)));
            if (traceParentHeaders.isEmpty() || !allHeadersEqual) {
                return null;
            }
        }
        String traceParentHeader = traceParentHeaders.get(0);
        return parseHeader(traceParentHeader);
    }

    static W3CTraceParent parseHeader(String traceParentHeader) {
        String[] traceParentFields = traceParentHeader.split(W3CTraceParentHeader.W3C_TRACE_PARENT_DELIMITER);
        if (traceParentFields.length < 4) {
            ServiceFactory.getStatsService().getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_PARENT_FIELD_COUNT);
            // We do not support any version that has less than 4 fields
            return null;
        }

        String version = traceParentFields[0];
        String traceId = traceParentFields[1];
        String parentId = traceParentFields[2];
        String unparsedFlags = traceParentFields[3];

        boolean valid = W3CTraceParentValidator.forHeader(traceParentHeader)
                .version(version)
                .traceId(traceId)
                .parentId(parentId)
                .flags(unparsedFlags)
                .isValid();

        if (!valid) {
            ServiceFactory.getStatsService().getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_PARENT_INVALID);
            // The payload was invalid and will be discarded
            return null;
        }

        int flags = Integer.parseInt(unparsedFlags, 16);
        return new W3CTraceParent(version, traceId, parentId, flags);
    }
}
