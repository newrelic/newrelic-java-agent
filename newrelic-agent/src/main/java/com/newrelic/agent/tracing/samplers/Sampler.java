package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.PriorityAware;

public interface Sampler {
    <T extends PriorityAware> boolean shouldSample(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir);
    <T extends PriorityAware> float calculatePriority(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir);
}
