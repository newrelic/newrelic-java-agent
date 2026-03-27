package com.newrelic.agent.tracing.samplers;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.tracing.Granularity;
import com.newrelic.api.agent.NewRelic;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class AdaptiveSampler implements Sampler {
    // Sentinel value for startTimeMillis indicating the sampling period has not yet started.
    private static final long UNSTARTED_PERIOD_SENTINEL = 0L;

    //Configured values
    private final long reportPeriodMillis;
    private int target;
    private final boolean isSharedInstance;

    //Instance stats - thread safety managed by synchronized methods
    private long startTimeMillis;
    private int seen;
    private int seenLast;
    private int sampledCount;
    private int sampledCountLast;
    private boolean firstPeriod;

    protected AdaptiveSampler(int target, int reportPeriodSeconds) {
        this(target, reportPeriodSeconds, false);
    }

    protected AdaptiveSampler(int target, int reportPeriodSeconds, boolean isSharedInstance) {
        this(target, reportPeriodSeconds, isSharedInstance, false);
    }

    AdaptiveSampler(int target, int reportPeriodSeconds, boolean isSharedInstance, boolean lazyStart) {
        this.target = target;
        this.reportPeriodMillis = reportPeriodSeconds * 1000L;
        this.isSharedInstance = isSharedInstance;
        // In a deferred-start mode, startTimeMillis is set to UNSTARTED_PERIOD_SENTINEL.
        // It will be set to the current time on the first calculatePriority() call.
        this.startTimeMillis = lazyStart ? UNSTARTED_PERIOD_SENTINEL : System.currentTimeMillis();
        this.seen = 0;
        this.seenLast = 0;
        this.sampledCount = 0;
        this.sampledCountLast = 0;
        this.firstPeriod = true;
        NewRelic.getAgent().getLogger().log(Level.INFO, "Started Adaptive Sampler with sampling target " + this.target + " and report period " +
                reportPeriodSeconds + " seconds" + (lazyStart ? " (lazy-start mode)" : "") + ".");
    }

    /**
     * Calculate priority given the current state of the sampler.
     *
     * @return A float in [0.0f, 2.0f]
     */
    @Override
    public synchronized float calculatePriority(Transaction tx, Granularity granularity) {
        resetPeriodIfElapsed();
        Float inboundPriority = tx.getPriorityFromInboundSamplingDecision(granularity);
        if (inboundPriority != null) {
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.FINEST, "Adaptive Sampler found an inbound priority for transaction {0}. A new sampling decision will not be made.", tx);
            return inboundPriority;
        }
        NewRelic.getAgent()
                .getLogger()
                .log(Level.FINEST, "Adaptive Sampler did not find an inbound priority for transaction {0}. A new sampling decision will be made.", tx);
        return DistributedTraceServiceImpl.nextTruncatedFloat() + (computeSampled() ? granularity.priorityIncrement() : 0.0f);
    }

    @Override
    public SamplerType getType() {
        return SamplerType.ADAPTIVE;
    }

    @Override
    public String getDescription() {
       return "Adaptive Sampler, shared=" + isShared() + ", target=" + target;
    }

    public boolean isShared(){
        return isSharedInstance;
    }

    private void resetPeriodIfElapsed() {
        long now = System.currentTimeMillis();
        if (startTimeMillis == UNSTARTED_PERIOD_SENTINEL) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "Adaptive Sampler lazy-start: anchoring period to first transaction.");
            startTimeMillis = now;
            return;
        }
        if (now - startTimeMillis >= reportPeriodMillis) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "Resetting sampler period. Seen: " + seen + ", Sampled: " + sampledCount);
            //Calculate elapsed periods so that the start time is consistently incremented
            //in multiples of the report period.
            int elapsedPeriods = (int) ((now - startTimeMillis) / reportPeriodMillis);
            startTimeMillis += elapsedPeriods * reportPeriodMillis;
            seenLast = seen;
            seen = 0;
            sampledCountLast = sampledCount;
            sampledCount = 0;
            firstPeriod = false;
        }
    }

    @VisibleForTesting
    protected boolean computeSampled() {
        boolean sampled;
        if (firstPeriod) {
            sampled = sampledCount < target;
        } else if (sampledCount < target) {
            sampled = (seenLast <= 0 ? 0 : ThreadLocalRandom.current().nextInt(seenLast)) < target;
        } else if (sampledCount >= (target * 2)) {
            sampled = false;
        } else {
            int expTarget = (int) (Math.pow((float) target, (float) target / sampledCount) - Math.pow((float) target, 0.5));
            //seen should never be zero here. This is an added safety guard to prevent an exception from .nextInt.
            sampled = (seen <= 0 ? 0 : ThreadLocalRandom.current().nextInt(seen)) < expTarget;
        }
        seen++;
        if (sampled) {
            sampledCount++;
        }
        return sampled;
    }

    protected synchronized void setTarget(int newTarget) {
        this.target = newTarget;
    }

    //These methods are for testing only. they are not thread-safe.
    @VisibleForTesting
    int getSampledCountLastPeriod() {
        return sampledCountLast;
    }

    @VisibleForTesting
    public int getTarget() {
        return target;
    }

    @VisibleForTesting
    long getStartTimeMillis() {
        return startTimeMillis;
    }
}
