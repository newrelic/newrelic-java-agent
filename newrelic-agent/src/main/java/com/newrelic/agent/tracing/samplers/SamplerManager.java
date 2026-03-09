package com.newrelic.agent.tracing.samplers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.coretracing.CoreTracingConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase;
import com.newrelic.agent.tracing.Granularity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * To support auto-app naming, more sophisticated management of the Samplers is now required.
 */
public class SamplerManager {

    private final CoreTracingConfig fullGranularityConfig;
    private final CoreTracingConfig partialGranularityConfig;
    private final String defaultAppName;
    private final Map<Granularity, SamplerMap> defaultSamplers;
    private final Map<String, Map<Granularity, SamplerMap>> samplersForApp = new ConcurrentHashMap<>();
    private final boolean autoAppNamingEnabled;

    public SamplerManager(DistributedTracingConfig config) {
        this.fullGranularityConfig = config.getFullGranularityConfig();
        this.partialGranularityConfig = config.getPartialGranularityConfig();
        this.defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        this.defaultSamplers = buildSamplersGranularityMap(defaultAppName);
        this.autoAppNamingEnabled = ServiceFactory.getConfigService().getDefaultAgentConfig().isAutoAppNamingEnabled();
    }

    public Sampler getDefaultSampler(Granularity granularity, SamplerCase samplerCase) {
        return getSampler(defaultAppName, granularity, samplerCase);
    }

    public Sampler getSampler(String appName, Granularity granularity, SamplerCase samplerCase) {
        if (!autoAppNamingEnabled || appName == null || defaultAppName.equals(appName)) {
            return defaultSamplers.get(granularity).getCase(samplerCase);
        }
        samplersForApp.computeIfAbsent(appName, k -> buildSamplersGranularityMap(appName));
        return samplersForApp.get(appName).get(granularity).getCase(samplerCase);
    }

    private Map<Granularity, SamplerMap> buildSamplersGranularityMap(String appName) {
        Map<Granularity, SamplerMap> map = new HashMap<>();
        map.put(Granularity.FULL, new SamplerMap(appName, fullGranularityConfig));
        map.put(Granularity.PARTIAL, new SamplerMap(appName, partialGranularityConfig));
        return map;
    }

    ////// TESTING METHODS ONLY!!!!!!! ///////
    /// THESE METHODS ARE NOT THREAD SAFE. THEY SHOULD NOT BE USED OUTSIDE OF TESTS.
    /// SETTING A SAMPLER IN-CODE IS NOT SUPPORTED. ONCE SAMPLERS HAVE BEEN BUILT,
    /// THEY SHOULD BE CONSIDERED FINAL.
    ///

    @VisibleForTesting
    public void setSampler(String appName, Granularity granularity, SamplerCase samplerCase, Sampler newSampler) {
        if (samplersForApp.get(appName) == null) {
            return;
        }
        SamplerMap samplers = samplersForApp.get(appName).get(granularity);
        samplers.replaceSampler(samplerCase, newSampler);
    }

    @VisibleForTesting
    public void setDefaultSampler(Granularity granularity, SamplerCase samplerCase, Sampler newSampler) {
        defaultSamplers.get(granularity).replaceSampler(samplerCase, newSampler);
    }

    //TODO REPLACE BESPOKE CLASS WITH A BARE-BONES DATA STRUCTURE FOR EFFICIENCY
    public class SamplerMap {
        private ImmutableMap<SamplerCase, Sampler> backingMap;

        SamplerMap(String appName, CoreTracingConfig config) {
            this.backingMap = samplerMapForConfig(appName, config);
        }

        public Sampler getCase(SamplerCase samplerCase) {
            return backingMap.get(samplerCase);
        }

        private ImmutableMap<SamplerCase, Sampler> samplerMapForConfig(String appName,CoreTracingConfig config) {
            return ImmutableMap.of(
                    SamplerCase.ROOT, SamplerFactory.createSampler(appName, config.getRootSampler()),
                    SamplerCase.REMOTE_PARENT_SAMPLED, SamplerFactory.createSampler(appName, config.getRemoteParentSampledSampler()),
                    SamplerCase.REMOTE_PARENT_NOT_SAMPLED, SamplerFactory.createSampler(appName, config.getRemoteParentNotSampledSampler())
            );
        }

        @VisibleForTesting
        void replaceSampler(SamplerCase samplerToReplace, Sampler newSampler) {
            Map<SamplerCase, Sampler> copy = new HashMap<>(backingMap);
            copy.put(samplerToReplace, newSampler);
            backingMap = ImmutableMap.copyOf(copy);
        }
    }
}
