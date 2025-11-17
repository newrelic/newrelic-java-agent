package com.newrelic.agent.config.coretracing;

import com.newrelic.api.agent.NewRelic;

import java.util.Map;
import java.util.logging.Level;

public class FullGranularityConfig extends CoreTracingConfig {

    public static final boolean FULL_GRANULARITY_ENABLED_DEFAULT = true;

    private final BaseSamplerCoreTracingConfig configDelegate;

    FullGranularityConfig(Map<String, Object> props, String samplerSystemPropertyPrefix, BaseSamplerCoreTracingConfig configDelegate) {
        super(props, samplerSystemPropertyPrefix + FULL_GRANULARITY + ".", FULL_GRANULARITY_ENABLED_DEFAULT);
        this.configDelegate = configDelegate;
    }

    @Override
    protected SamplerConfig createSamplerConfig(String samplerCase){
        SamplerConfig sampler = new SamplerConfig(samplerCase, getProperties(), systemPropertyPrefix, configDelegate.getSamplerConfigForCase(samplerCase));
        NewRelic.getAgent()
                .getLogger()
                .log(Level.INFO,
                        "After merging with base sampler settings, the full granularity " + samplerCase + " sampler was configured to use the " +
                                sampler.getSamplerType() + " sampler type" +
                                (sampler.getSamplerRatio() != null ? " with a ratio of " + sampler.getSamplerRatio() : "") +
                                (sampler.getSamplingTarget() != null ? " with a target of " + sampler.getSamplingTarget() : "") + ".");
        return sampler;
    }
}
