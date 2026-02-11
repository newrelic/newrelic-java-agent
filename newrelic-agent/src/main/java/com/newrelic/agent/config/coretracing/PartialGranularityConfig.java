/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.coretracing;

import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Transaction.PartialSampleType;
import com.newrelic.api.agent.NewRelic;

public class PartialGranularityConfig extends CoreTracingConfig {

    public static final String TYPE = "type";

    //partial granularity type options
    public static final String REDUCED = "reduced";
    public static final String ESSENTIAL = "essential";
    public static final String COMPACT = "compact";

    //defaults
    public static final boolean PARTIAL_GRANULARITY_ENABLED_DEFAULT = false;
    public static final String PARTIAL_GRANULARITY_DEFAULT_TYPE = ESSENTIAL;

    private final PartialSampleType type;

    private final BaseSamplerCoreTracingConfig fullGranularityConfig;

    public PartialGranularityConfig(Map<String, Object> props, String samplerSystemPropertyRoot, BaseSamplerCoreTracingConfig fullGranularityConfig) {
        super(props, samplerSystemPropertyRoot + CoreTracingConfig.PARTIAL_GRANULARITY + ".", PARTIAL_GRANULARITY_ENABLED_DEFAULT);
        this.type = initType();
        this.fullGranularityConfig = fullGranularityConfig;
    }

    @Override
    public SamplerConfig createSamplerConfig(String samplerCase) {
        SamplerConfig sampler = super.createSamplerConfig(samplerCase);
        SamplerConfig fullSampler = fullGranularityConfig.getSamplerConfigForCase(samplerCase);
        if (sampler.getSamplerRatio() != null && fullSampler.getSamplerRatio() != null && fullGranularityConfig.isEnabled()) {
            Float originalRatio = sampler.getSamplerRatio();
            sampler.setRatio(originalRatio + fullSampler.getSamplerRatio());
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.FINE,
                            "Partial granularity {0} sampler was originally configured with ratio={1}. " +
                                    "Added to full granularity ratio={2} and set effective ratio={3}",
                            samplerCase, originalRatio, fullSampler.getSamplerRatio(), sampler.getSamplerRatio());
        }
        if (isEnabled()){
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.INFO,
                            "The partial granularity " + samplerCase + " sampler was configured to use the " +
                                    sampler.getSamplerType() + " sampler type" +
                                    (sampler.getSamplerRatio() != null ? " with a ratio of " + sampler.getSamplerRatio() : "") +
                                    (sampler.getSamplingTarget() != null ? " with a target of " + sampler.getSamplingTarget() : "") + ".");
        }
        return sampler;
    }

    public PartialSampleType getType() {
        return type;
    }

    private PartialSampleType initType() {
        switch (getProperty(TYPE, PARTIAL_GRANULARITY_DEFAULT_TYPE)) {
            case ESSENTIAL: return PartialSampleType.ESSENTIAL;
            case REDUCED: return PartialSampleType.REDUCED;
            case COMPACT: return PartialSampleType.COMPACT;
            default: return PartialSampleType.ESSENTIAL;
        }
    }
}
