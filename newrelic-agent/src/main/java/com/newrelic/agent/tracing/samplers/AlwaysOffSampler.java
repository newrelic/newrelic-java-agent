package com.newrelic.agent.tracing.samplers;

public class AlwaysOffSampler implements Sampler{
    public float calculatePriority(){
        return 0.0f;
    }

    public String getType(){
        return Sampler.ALWAYS_OFF;
    }
}
