package com.newrelic.agent.tracing.samplers;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;

import java.util.concurrent.ThreadLocalRandom;

public class AdaptiveSampler implements Sampler {
    //Configured values
    private final long REPORT_PERIOD_MILLIS;
    private final int TARGET;

    //Instance stats - thread safety managed by synchronized methods
    private long startTimeMillis;
    private int seen;
    private int seenLast;
    private int sampledCount;
    private int sampledCountLast;
    private boolean firstPeriod;

    private static AdaptiveSampler SAMPLER_SHARED_INSTANCE;

    protected AdaptiveSampler(int target, int reportPeriodSeconds){
        this.TARGET = target;
        this.REPORT_PERIOD_MILLIS = reportPeriodSeconds * 1000L;
        this.startTimeMillis = System.currentTimeMillis();
        this.seen = 0;
        this.seenLast = 0;
        this.sampledCount = 0;
        this.sampledCountLast = 0;
        this.firstPeriod = true;
        System.out.println("Initialized sampler with target: " + target + " and period: " + reportPeriodSeconds);
    }

    /**
     * Factory method for getting a shared instance of the adaptive sampler.
     * This is the instance used when a top-level sampling target only is specified.
     * Its state may be shared across multiple contexts using adaptive sampling, which is why
     * it is a singleton.
     *
     * Lazy-instantiated.
     * Currently managed via synchronized as it should only be accessed a few times,
     * when DistributedTraceImpl class is initialized.
     *
     * @return The AdaptiveSampler instance.
     */
    public static synchronized AdaptiveSampler getSharedInstance(){
        if (SAMPLER_SHARED_INSTANCE == null) {
            AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
            SAMPLER_SHARED_INSTANCE = new AdaptiveSampler(config.getAdaptiveSamplingTarget(), config.getAdaptiveSamplingPeriodSeconds());
        }
        return SAMPLER_SHARED_INSTANCE;
    }

    /**
     * Calculate priority given the current state of the sampler.
     * @return A float in [0.0f, 2.0f]
     */
    @Override
    public synchronized float calculatePriority(){
        resetPeriodIfElapsed();
        return (computeSampled() ? 1.0f : 0.0f) + DistributedTraceServiceImpl.nextTruncatedFloat();
    }

    @Override
    public String getType(){
        return Sampler.ADAPTIVE;
    }

    private void resetPeriodIfElapsed(){
        long now = System.currentTimeMillis();
        if (now - startTimeMillis >= REPORT_PERIOD_MILLIS) {
            System.out.println("Resetting sampler period. Seen: " + seen + " Sampled: " + sampledCount);
            //Calculate elapsed periods so that the start time is consistently incremented
            //in multiples of the report period.
            int elapsedPeriods = (int) ((now - startTimeMillis)/ REPORT_PERIOD_MILLIS);
            startTimeMillis += elapsedPeriods * REPORT_PERIOD_MILLIS;
            seenLast = seen;
            seen = 0;
            sampledCountLast = sampledCount;
            sampledCount = 0;
            firstPeriod = false;
        }
    }

    @VisibleForTesting
    protected boolean computeSampled(){
        boolean sampled;
        if (firstPeriod) {
            sampled = sampledCount < TARGET;
        } else if (sampledCount < TARGET) {
            sampled = (seenLast <= 0 ? 0 : ThreadLocalRandom.current().nextInt(seenLast)) < TARGET;
        } else if (sampledCount >= (TARGET * 2)) {
            sampled = false;
        } else {
            int expTarget = (int) (Math.pow((float) TARGET, (float) TARGET / sampledCount) - Math.pow((float) TARGET, 0.5));
            //seen should never be zero here. This is an added safety guard to prevent an exception from .nextInt.
            sampled = (seen <= 0 ? 0 : ThreadLocalRandom.current().nextInt(seen)) < expTarget;
        }
        seen++;
        if (sampled){
            sampledCount++;
        }
        return sampled;
    }

    //These methods are for testing only. they are not thread-safe.
    int getSampledCountLastPeriod(){
        return sampledCountLast;
    }

}
