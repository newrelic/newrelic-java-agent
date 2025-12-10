package com.newrelic.agent.tracing.samplers;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.tracing.Granularity;
import com.newrelic.api.agent.NewRelic;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class AdaptiveSampler implements Sampler {
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

    private static AdaptiveSampler SAMPLER_SHARED_INSTANCE;

    /**
     * Package-protected constructor for creating non-shared AdaptiveSampler instances.
     * <p>
     * This constructor always creates non-shared instances (isSharedInstance=false).
     * To obtain the shared singleton instance, use {@link #getSharedInstance()} instead.
     * External callers should use {@link #getAdaptiveSampler(SamplerConfig)}.
     *
     * @param target the sampling target
     * @param reportPeriodSeconds the reporting period in seconds
     */

    protected AdaptiveSampler(int target, int reportPeriodSeconds) {
        this(target, reportPeriodSeconds, false);
    }

    private AdaptiveSampler(int target, int reportPeriodSeconds, boolean isSharedInstance) {
        this.target = target;
        this.reportPeriodMillis = reportPeriodSeconds * 1000L;
        this.startTimeMillis = System.currentTimeMillis();
        this.isSharedInstance = isSharedInstance;
        this.seen = 0;
        this.seenLast = 0;
        this.sampledCount = 0;
        this.sampledCountLast = 0;
        this.firstPeriod = true;
        NewRelic.getAgent().getLogger().log(Level.INFO, "Started Adaptive Sampler with sampling target " + this.target + " and report period " +
                reportPeriodSeconds + " seconds.");
    }

    /**
     * Factory method for getting a shared instance of the adaptive sampler.
     * This is the instance used when a top-level sampling target only is specified.
     * Its state may be shared across multiple contexts using adaptive sampling, which is why
     * it is a singleton.
     * <p>
     * Lazy-instantiated.
     * Currently managed via synchronized as it should only be accessed a few times,
     * when DistributedTraceImpl class is initialized.
     *
     * @return The AdaptiveSampler instance.
     */
    public static synchronized AdaptiveSampler getSharedInstance() {
        if (SAMPLER_SHARED_INSTANCE == null) {
            AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
            SAMPLER_SHARED_INSTANCE = new AdaptiveSampler(config.getAdaptiveSamplingTarget(), config.getAdaptiveSamplingPeriodSeconds(), true);
        }
        return SAMPLER_SHARED_INSTANCE;
    }

    public static AdaptiveSampler getAdaptiveSampler(SamplerConfig config) {
        Integer target = config.getSamplingTarget();
        if (target == null) {
            return getSharedInstance();
        } else {
            return new AdaptiveSampler(target, ServiceFactory.getConfigService().getDefaultAgentConfig().getAdaptiveSamplingPeriodSeconds());
        }
    }

    /**
     * Updates the SHARED_SAMPLER_INSTANCE to use a new target.
     * If the SHARED_SAMPLER_INSTANCE isn't already running, this method is a no-op.
     *
     * @param newTarget the new target value the shared sampler instance should use
     */
    public static synchronized void setSharedTarget(int newTarget) {
        if (SAMPLER_SHARED_INSTANCE != null) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "Updating shared Adaptive Sampler sampling target to " + newTarget);
            getSharedInstance().setTarget(newTarget);

            ServiceFactory.getStatsService()
                    .doStatsWork(
                            StatsWorks.getRecordMetricWork(MetricNames.SUPPORTABILITY_TRACE_SAMPLING_TARGET_APPLIED_VALUE, ((Number) newTarget).floatValue()),
                            MetricNames.SUPPORTABILITY_TRACE_SAMPLING_TARGET_APPLIED_VALUE);
        }
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

    private synchronized void setTarget(int newTarget) {
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

}
