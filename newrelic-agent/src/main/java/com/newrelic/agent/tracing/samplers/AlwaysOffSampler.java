package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;

public class AlwaysOffSampler implements Sampler {
    @Override
    public float calculatePriority(Transaction tx){
        return 0.0f;
    }

    @Override
    public SamplerType getType(){
        return SamplerType.ALWAYS_OFF;
    }

    @Override
    public String getDescription(){
        return "Always Off Sampler";
    }
}
