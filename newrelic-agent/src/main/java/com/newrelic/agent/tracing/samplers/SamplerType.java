package com.newrelic.agent.tracing.samplers;

import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

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
