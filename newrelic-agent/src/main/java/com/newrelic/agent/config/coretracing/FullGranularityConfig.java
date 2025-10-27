package com.newrelic.agent.config.coretracing;

import java.util.Map;

public class FullGranularityConfig extends CoreTracingConfig {

    public static final boolean FULL_GRANULARITY_ENABLED_DEFAULT = true;

    private final BaseSamplerCoreTracingConfig configDelegate;

    FullGranularityConfig(Map<String, Object> props, String samplerSystemPropertyPrefix, BaseSamplerCoreTracingConfig configDelegate) {
        super(props, samplerSystemPropertyPrefix + FULL_GRANULARITY + ".", FULL_GRANULARITY_ENABLED_DEFAULT);
        this.configDelegate = configDelegate;
    }

    @Override
    protected SamplerConfig createSamplerConfig(String samplerCase){
        return new SamplerConfig(samplerCase, getProperties(), systemPropertyPrefix, configDelegate.getSamplerConfigForCase(samplerCase));
    }
}
