/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing.samplers;

public enum SamplerType {
    ADAPTIVE("Adaptive"),
    ALWAYS_ON("AlwaysOn"),
    ALWAYS_OFF("AlwaysOff"),
    TRACE_ID_RATIO_BASED("TraceIdRatioBased"),
    PROBABILITY("Probability");

    private final String displayName;

    SamplerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
