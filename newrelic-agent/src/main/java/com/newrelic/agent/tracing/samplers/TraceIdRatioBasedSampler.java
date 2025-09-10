package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.PriorityAware;

public class TraceIdRatioBasedSampler implements Sampler {

    private final long THRESHOLD_LONG;

    public TraceIdRatioBasedSampler(Float traceRatio) {
        long MAX = Long.MAX_VALUE;
        this.THRESHOLD_LONG = (long) (MAX * traceRatio);
    }

    public static boolean validRatio(float ratio){
        return true;
    }

    @Override
    public <T extends PriorityAware> boolean shouldSample(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir){
        return shouldSample(traceId);
    }

    @Override
    public <T extends PriorityAware> float calculatePriority(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir){
        return calculatePriority(traceId);
    }

    public boolean shouldSample(String traceId){;
        return Math.abs(getLongForTraceId(traceId)) <= THRESHOLD_LONG;
    }

    private float calculatePriority(String traceId){
        return shouldSample(traceId) ? 2.0f : 0.0f;
    }

    private long getLongForTraceId(String traceId){
        String last16Chars = traceId.substring(16);
        return Long.parseUnsignedLong(last16Chars, 16);
    }
}
