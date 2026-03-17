package com.newrelic.agent.tracing.samplers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.coretracing.CoreTracingConfig;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase;
import com.newrelic.agent.tracing.Granularity;
import com.newrelic.api.agent.NewRelic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * The SamplerManager is responsible for managing the lifecycle of every sampler used by the agent.
 * A SamplerManager instance is created by the DistributedTraceServiceImpl (of which there should be one active per JVM).
 * <p>
 * The SamplerManager manages the Samplers for every appName it encounters. In standard (non auto-app-naming) scenarios, these are just the Samplers for the default app.
 * When auto-app-naming is enabled, a new SamplerCollection is constructed for each appName (actual Sampler instances are not constructed,
 * except in the case of AdaptiveSamplers - see the javadoc for SamplerCollection for more information on how Samplers are shared across apps).
 */
public class SamplerManager {
    private static final String UNKNOWN_APP_NAME = "UNKNOWN_APP_NAME";

    private final boolean autoAppNamingEnabled;
    private final CoreTracingConfig fullGranularityConfig;
    private final CoreTracingConfig partialGranularityConfig;
    private final String defaultAppName;
    private final int adaptiveSamplingPeriod;

    private volatile int sharedAdaptiveSamplingTarget;

    private final SamplerCollection defaultSamplers;
    private final Map<String, SamplerCollection> samplersForApp = new ConcurrentHashMap<>();


    public SamplerManager(DistributedTracingConfig dtConfig) {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        this.autoAppNamingEnabled = agentConfig.isAutoAppNamingEnabled();
        this.defaultAppName = agentConfig.getApplicationName() == null ? UNKNOWN_APP_NAME : agentConfig.getApplicationName();
        this.adaptiveSamplingPeriod = agentConfig.getAdaptiveSamplingPeriodSeconds();
        this.sharedAdaptiveSamplingTarget = dtConfig.getAdaptiveSamplingTarget();
        this.fullGranularityConfig = dtConfig.getFullGranularityConfig();
        this.partialGranularityConfig = dtConfig.getPartialGranularityConfig();

        this.defaultSamplers = new SamplerCollection(true);
    }

    public Sampler getSampler(String appName, Granularity granularity, SamplerCase samplerCase) {
        if (!autoAppNamingEnabled || appName == null || defaultAppName.equals(appName)) {
            return defaultSamplers.get(granularity, samplerCase);
        }
        SamplerCollection samplers = samplersForApp.computeIfAbsent(appName, k -> new SamplerCollection(false));
        return samplers.get(granularity, samplerCase);
    }

    public Sampler getSampler(Granularity granularity, SamplerCase samplerCase) {
        return getSampler(defaultAppName, granularity, samplerCase);
    }

    public void setSharedSamplingTargets(int newTarget){
        this.sharedAdaptiveSamplingTarget = newTarget;
        NewRelic.getAgent().getLogger().log(Level.INFO, "Updating shared Adaptive Sampler sampling target to " + newTarget);
        defaultSamplers.setSharedSamplingTarget(newTarget);
        for (SamplerCollection samplers : samplersForApp.values()) {
            samplers.setSharedSamplingTarget(newTarget);
        }
        ServiceFactory.getStatsService()
                .doStatsWork(
                        StatsWorks.getRecordMetricWork(MetricNames.SUPPORTABILITY_TRACE_SAMPLING_TARGET_APPLIED_VALUE, ((Number) newTarget).floatValue()),
                        MetricNames.SUPPORTABILITY_TRACE_SAMPLING_TARGET_APPLIED_VALUE);
    }


    /**
     * Represents the collection of six samplers (Full/Partial Granularity) x (Root/RemoteParentSampled/RemoteParentNotSampled)
     * for a single application.
     * <p>
     * This class is responsible for creating or sharing Sampler instances as follows:
     * <p>
     * Default app: Creates the main set of Sampler instances.
     * Auto-named apps: Reuses references to the default app's Samplers, EXCEPT for AdaptiveSamplers, which must always be created per-app.
     * The sharing strategy is an optimization, since all other Samplers (AlwaysOn, AlwaysOff, TraceIdRatioBased) do not need to be app-aware.
     * It makes the implementation a little more confusing. The "sharing" logic is visible in reuseDefaultOrCreateAdaptiveSampler.
     * <p>
     * Every SamplerCollection also includes a sharedAdaptiveSampler instance, which represents the application's "global" adaptive sampler that may be
     * shared across Full and Partial Granularity samplers of all cases.
     */
    private class SamplerCollection {
        private ImmutableMap<SamplerCase, Sampler> fullGranularitySamplers;
        private ImmutableMap<SamplerCase, Sampler> partialGranularitySamplers;
        //The global, shared adaptive sampler for this app. This is currently set up for every app even if the app is configured to use other samplers.
        private final AdaptiveSampler sharedAdaptiveSampler;

        SamplerCollection(boolean isDefaultApp){
            this.sharedAdaptiveSampler = new AdaptiveSampler(sharedAdaptiveSamplingTarget, adaptiveSamplingPeriod, true);
            this.fullGranularitySamplers = isDefaultApp ? samplersForDefaultApp(Granularity.FULL) : samplersForAutoNamedApp(Granularity.FULL);
            this.partialGranularitySamplers = isDefaultApp ? samplersForDefaultApp(Granularity.PARTIAL) : samplersForAutoNamedApp(Granularity.PARTIAL);
        }

        Sampler get(Granularity granularity, SamplerCase samplerCase) {
            return granularity == Granularity.FULL ? fullGranularitySamplers.get(samplerCase) : partialGranularitySamplers.get(samplerCase);
        }

        void setSharedSamplingTarget(int newTarget) {
            sharedAdaptiveSampler.setTarget(newTarget);
        }

        private ImmutableMap<SamplerCase, Sampler> samplersForDefaultApp(Granularity granularity) {
            CoreTracingConfig config = granularity == Granularity.FULL ? fullGranularityConfig : partialGranularityConfig;
            return ImmutableMap.of(
                    SamplerCase.ROOT, createDefaultSampler(config.getRootSampler()),
                    SamplerCase.REMOTE_PARENT_SAMPLED, createDefaultSampler(config.getRemoteParentSampledSampler()),
                    SamplerCase.REMOTE_PARENT_NOT_SAMPLED, createDefaultSampler(config.getRemoteParentNotSampledSampler())
            );
        }

        private ImmutableMap<SamplerCase, Sampler> samplersForAutoNamedApp(Granularity granularity) {
            CoreTracingConfig config = granularity == Granularity.FULL ? fullGranularityConfig : partialGranularityConfig;
            return ImmutableMap.of(
                    SamplerCase.ROOT, reuseDefaultOrCreateAdaptiveSampler(config.getRootSampler(), granularity, SamplerCase.ROOT),
                    SamplerCase.REMOTE_PARENT_SAMPLED, reuseDefaultOrCreateAdaptiveSampler(config.getRemoteParentSampledSampler(), granularity, SamplerCase.REMOTE_PARENT_SAMPLED),
                    SamplerCase.REMOTE_PARENT_NOT_SAMPLED, reuseDefaultOrCreateAdaptiveSampler(config.getRemoteParentNotSampledSampler(), granularity, SamplerCase.REMOTE_PARENT_NOT_SAMPLED)
            );
        }

        private Sampler createDefaultSampler(SamplerConfig samplerConfig){
            switch (samplerConfig.getSamplerType()) {
                case SamplerConfig.ALWAYS_ON:
                    return new AlwaysOnSampler();
                case SamplerConfig.ALWAYS_OFF:
                    return new AlwaysOffSampler();
                case SamplerConfig.TRACE_ID_RATIO_BASED:
                    return new TraceRatioBasedSampler(samplerConfig);
                default:
                    return createAdaptiveSampler(samplerConfig);
            }
        }

        private Sampler reuseDefaultOrCreateAdaptiveSampler(SamplerConfig config, Granularity granularity, SamplerCase samplerCase) {
            if (config.getSamplerType().equals(SamplerConfig.ADAPTIVE)){
                return createAdaptiveSampler(config);
            } else {
                return defaultSamplers.get(granularity, samplerCase);
            }
        }

        private AdaptiveSampler createAdaptiveSampler(SamplerConfig samplerConfig){
            Integer target = samplerConfig.getSamplingTarget();
            if (target == null) {
                return sharedAdaptiveSampler;
            } else {
                return new AdaptiveSampler(target, adaptiveSamplingPeriod);
            }
        }

        //Test-only method.
        @VisibleForTesting
        void replaceSampler(Granularity granularity, SamplerCase samplerCase, Sampler newSampler) {
            if (granularity == Granularity.FULL) {
                Map<SamplerCase, Sampler> newSamplers = new HashMap<>(fullGranularitySamplers);
                newSamplers.put(samplerCase, newSampler);
                this.fullGranularitySamplers = ImmutableMap.copyOf(newSamplers);
            } else {
                Map<SamplerCase, Sampler> newSamplers = new HashMap<>(partialGranularitySamplers);
                newSamplers.put(samplerCase, newSampler);
                this.partialGranularitySamplers = ImmutableMap.copyOf(newSamplers);
            }
        }
    }


    //====== Testing-Only Methods =========
    // These methods are NOT thread safe. They should NOT be used outside of tests.

    @VisibleForTesting
    public void setSampler(String appName, Granularity granularity, SamplerCase samplerCase, Sampler newSampler) {
        if (samplersForApp.get(appName) == null) {
            return;
        }
        SamplerCollection samplers = samplersForApp.get(appName);
        samplers.replaceSampler(granularity, samplerCase, newSampler);
    }

    @VisibleForTesting
    public void setDefaultSampler(Granularity granularity, SamplerCase samplerCase, Sampler newSampler) {
        defaultSamplers.replaceSampler(granularity, samplerCase, newSampler);
    }
}
