package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.PriorityAware;

public class AlwaysOnSampler implements Sampler{

    @Override
    public <T extends PriorityAware> boolean shouldSample(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir){
        return shouldSample();
    }

    @Override
    public <T extends PriorityAware> float calculatePriority(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir){
        return calculatePriority();
    }

    public boolean shouldSample(){
        return true;
    }

    public float calculatePriority(){
        return 2.0f;
    }
}
