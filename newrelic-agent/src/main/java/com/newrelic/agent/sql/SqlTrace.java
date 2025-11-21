/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TraceMetadata;

import java.util.Map;

public interface SqlTrace {

    long getId();

    String getQuery();

    int getCallCount();

    long getTotal();

    long getMax();

    long getMin();

    String getBlameMetricName();

    String getUri();

    String getMetricName();

    Map<String, Object> getParameters();

    static String createMetadataSqlComment() {
        if (NewRelic.getAgent().getTransaction() != NoOpTransaction.INSTANCE) {
            TraceMetadata traceMetadata = NewRelic.getAgent().getTraceMetadata();
            return String.format("/* nr_trace_id=%s,nr_span_id=%s,nr_service=%s */ ", traceMetadata.getTraceId(), traceMetadata.getSpanId(),
                    NewRelic.getAgent().getConfig().getValue("app_name"));
        }

        return "";
    }
}
