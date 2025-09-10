package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.PriorityAware;

public class AlwaysOffSampler implements Sampler{

    @Override
    public <T extends PriorityAware> boolean shouldSample(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir){
        return shouldSample();
    }

    @Override
    public <T extends PriorityAware> float calculatePriority(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir){
        return calculatePriority();
    }

    private boolean shouldSample(){
        return false;
    }

    private float calculatePriority(){
        return 0.0f;
    }
}
