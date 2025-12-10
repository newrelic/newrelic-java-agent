package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracing.DistributedTraceUtil;
import com.newrelic.agent.tracing.Granularity;

public class AlwaysOnSampler implements Sampler {
    @Override
    public float calculatePriority(Transaction tx,  Granularity granularity) {
        return 1.0f + granularity.priorityIncrement();
    }

    @Override
    public SamplerType getType(){
        return SamplerType.ALWAYS_ON;
    }

    @Override
    public String getDescription(){
        return "Always On Sampler";
    }
}
