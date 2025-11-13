package com.newrelic.agent.config.coretracing;

import java.util.Map;

public class BaseSamplerCoreTracingConfig extends CoreTracingConfig {
    private static final String SAMPLER_SYSTEM_PROPERTY_ROOT = "sampler.";
    public static final String SHARED_ADAPTIVE_SAMPLING_TARGET = "adaptive_sampling_target";
    public static final int SHARED_ADAPTIVE_SAMPLING_TARGET_DEFAULT = 120;

    public static final boolean BASE_SAMPLER_ENABLED_DEFAULT = true;

    private final int sharedAdaptiveSamplingTarget;
    private final FullGranularityConfig fullGranularityConfig;
    private final PartialGranularityConfig partialGranularityConfig;

    public BaseSamplerCoreTracingConfig(Map<String, Object> props, String dtSystemPropertyRoot) {
        super(props, dtSystemPropertyRoot + SAMPLER_SYSTEM_PROPERTY_ROOT, BASE_SAMPLER_ENABLED_DEFAULT);
        this.fullGranularityConfig = new FullGranularityConfig(nestedProps(FULL_GRANULARITY), this.systemPropertyPrefix, this);
        this.partialGranularityConfig = new PartialGranularityConfig(nestedProps(PARTIAL_GRANULARITY), this.systemPropertyPrefix);
        this.sharedAdaptiveSamplingTarget = getProperty(SHARED_ADAPTIVE_SAMPLING_TARGET, SHARED_ADAPTIVE_SAMPLING_TARGET_DEFAULT);
    }

    public int getSharedAdaptiveSamplingTarget(){
        return sharedAdaptiveSamplingTarget;
    }

    public FullGranularityConfig getFullGranularityConfig(){
        return fullGranularityConfig;
    }

    public PartialGranularityConfig getPartialGranularityConfig(){
        return partialGranularityConfig;
    }
}
