package com.newrelic.agent.tracing.samplers;

public class AlwaysOnSampler implements Sampler{

    public float calculatePriority(){
        return 2.0f;
    }

    public String getType(){
        return Sampler.ALWAYS_ON;
    }

}
