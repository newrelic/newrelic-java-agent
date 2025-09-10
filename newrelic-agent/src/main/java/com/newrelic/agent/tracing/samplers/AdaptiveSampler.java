package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.PriorityAware;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;

import java.util.concurrent.ThreadLocalRandom;

public class AdaptiveSampler implements Sampler{

    private final DistributedTraceServiceImpl dtService;

    public AdaptiveSampler(DistributedTraceServiceImpl dtService) {
        this.dtService = dtService;
    }

    @Override
    public <T extends PriorityAware> boolean shouldSample(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir){
        return shouldSample(reservoir);
    }

    @Override
    public <T extends PriorityAware> float calculatePriority(String traceId, Float inboundPriority, SamplingPriorityQueue<T> reservoir){
        return calculatePriority(inboundPriority, reservoir);
    }

    private <T extends PriorityAware> boolean shouldSample(SamplingPriorityQueue<T> reservoir){
        if (reservoir == null) {
            return false;
        }
        int seen = reservoir.getNumberOfTries();
        if (dtService.firstHarvest.get() || seen == 0) {
            // Make sure we record the first 'target' events in the system
            return seen <= reservoir.getTarget();
        }
        //after this point, we apply the actual sampling algorithm.
        int seenLast = reservoir.getDecidedLast();
        int sampled = reservoir.getSampled();
        int target = reservoir.getTarget();
        boolean shouldSample;
        if (sampled < target) {
            shouldSample = (seenLast <= 0 ? 0 : ThreadLocalRandom.current().nextInt(seenLast)) < target;
        } else if (sampled > (target * 2)) {
            // As soon as we hit 2x the number of "target" events sampled, we need to stop
            shouldSample = false;
        } else {
            int expTarget = (int) (Math.pow((float) target, (float) target / sampled) - Math.pow((float) target, 0.5));
            shouldSample = ThreadLocalRandom.current().nextInt(seen) < expTarget;
        }
        return shouldSample;
    }

    private <T extends PriorityAware> float calculatePriority(Float inboundPriority, SamplingPriorityQueue<T> reservoir){
        if (inboundPriority == null) {
            return (shouldSample(reservoir) ? 1.0f : 0.0f) + DistributedTraceServiceImpl.nextTruncatedFloat();
        } else {
            return inboundPriority;
        }
    }
}
