package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;

public class AlwaysOnSampler implements Sampler {
    @Override
    public float calculatePriority(Transaction tx){
        return 2.0f;
    }

    @Override
    public String getType(){
        return SamplerFactory.ALWAYS_ON;
    }

    @Override
    public String getDescription(){
        return "Always On Sampler";
    }
}
