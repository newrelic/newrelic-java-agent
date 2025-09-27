package com.newrelic.agent.tracing.samplers;

public interface Sampler {
    String ADAPTIVE = "adaptive";
    String ALWAYS_OFF = "always_off";
    String ALWAYS_ON = "always_on";

    float calculatePriority();
    String getType();
}
