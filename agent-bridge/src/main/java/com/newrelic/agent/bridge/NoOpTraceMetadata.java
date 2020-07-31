/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.TraceMetadata;

public class NoOpTraceMetadata implements TraceMetadata {

    static final TraceMetadata INSTANCE = new NoOpTraceMetadata();

    private NoOpTraceMetadata() {
    }

    @Override
    public String getTraceId() {
        return "";
    }

    @Override
    public String getSpanId() {
        return "";
    }

    @Override
    public boolean isSampled() {
        return false;
    }

}
