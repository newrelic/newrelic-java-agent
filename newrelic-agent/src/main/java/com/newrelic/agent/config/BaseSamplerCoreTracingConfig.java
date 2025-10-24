package com.newrelic.agent.config;

import java.util.Map;

public class BaseSamplerCoreTracingConfig extends CoreTracingConfig {
    private static final String SAMPLER_SYSTEM_PROPERTY_ROOT = "sampler.";
    public static final String SHARED_ADAPTIVE_SAMPLING_TARGET = "adaptive_sampling_target";
    public static final int SHARED_ADAPTIVE_SAMPLING_TARGET_DEFAULT = 120;

    private final int sharedAdaptiveSamplingTarget;
    private final CoreTracingConfig fullGranularityConfig;
    private final CoreTracingConfig partialGranularityConfig;

    public BaseSamplerCoreTracingConfig(Map<String, Object> props, String dtSystemPropertyRoot) {
        super(props, dtSystemPropertyRoot + SAMPLER_SYSTEM_PROPERTY_ROOT, BASE);
        this.fullGranularityConfig = new CoreTracingConfig(nestedProps(FULL_GRANULARITY), this.systemPropertyPrefix, FULL_GRANULARITY, this);
        this.partialGranularityConfig = new CoreTracingConfig(nestedProps(PARTIAL_GRANULARITY), this.systemPropertyPrefix , PARTIAL_GRANULARITY);
        this.sharedAdaptiveSamplingTarget = getProperty(SHARED_ADAPTIVE_SAMPLING_TARGET, SHARED_ADAPTIVE_SAMPLING_TARGET_DEFAULT);
    }

    public int getSharedAdaptiveSamplingTarget(){
        return sharedAdaptiveSamplingTarget;
    }

    public CoreTracingConfig getFullGranularityConfig(){
        return fullGranularityConfig;
    }

    public CoreTracingConfig getPartialGranularityConfig(){
        return partialGranularityConfig;
    }
}
