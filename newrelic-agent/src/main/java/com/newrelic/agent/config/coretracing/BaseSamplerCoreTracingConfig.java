/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.coretracing;

import com.newrelic.agent.config.BaseConfig;
import com.newrelic.api.agent.NewRelic;

import java.util.Map;
import java.util.logging.Level;

public class BaseSamplerCoreTracingConfig extends CoreTracingConfig {
    private static final String SAMPLER_SYSTEM_PROPERTY_ROOT = "sampler.";
    public static final String SHARED_ADAPTIVE_SAMPLING_TARGET = "adaptive_sampling_target";
    public static final int SHARED_ADAPTIVE_SAMPLING_TARGET_DEFAULT = 120;
    public static final String FULL_GRANULARITY_SYSTEM_PROPERTY_ROOT = "full_granularity.";

    public static final boolean BASE_SAMPLER_ENABLED_DEFAULT = true;
    public static final boolean FULL_GRANULARITY_ENABLED_DEFAULT = true;

    private final int sharedAdaptiveSamplingTarget;
    private final PartialGranularityConfig partialGranularityConfig;
    private final boolean isFullGranularityEnabled;

    public BaseSamplerCoreTracingConfig(Map<String, Object> props, String dtSystemPropertyRoot) {
        super(props, dtSystemPropertyRoot + SAMPLER_SYSTEM_PROPERTY_ROOT, BASE_SAMPLER_ENABLED_DEFAULT);
        //there is no sampler.enabled property that we honor. The full granularity enabled property is at sampler.full_granularity.enabled.
        this.isFullGranularityEnabled = (new BaseConfig(nestedProps(FULL_GRANULARITY), dtSystemPropertyRoot + SAMPLER_SYSTEM_PROPERTY_ROOT + FULL_GRANULARITY_SYSTEM_PROPERTY_ROOT)).getProperty(ENABLED, FULL_GRANULARITY_ENABLED_DEFAULT);
        this.sharedAdaptiveSamplingTarget = getProperty(SHARED_ADAPTIVE_SAMPLING_TARGET, SHARED_ADAPTIVE_SAMPLING_TARGET_DEFAULT);
        this.partialGranularityConfig = new PartialGranularityConfig(nestedProps(PARTIAL_GRANULARITY), this.systemPropertyPrefix, this);
    }

    public int getSharedAdaptiveSamplingTarget(){
        return sharedAdaptiveSamplingTarget;
    }

    public BaseSamplerCoreTracingConfig getFullGranularityConfig(){
        return this;
    }

    public PartialGranularityConfig getPartialGranularityConfig(){
        return partialGranularityConfig;
    }

    @Override
    public boolean isEnabled() {
        return isFullGranularityEnabled;
    }

    @Override
    public SamplerConfig createSamplerConfig(String samplerCase){
        SamplerConfig sampler = super.createSamplerConfig(samplerCase);
        NewRelic.getAgent()
                .getLogger()
                .log(Level.INFO,
                        "The full granularity " + samplerCase + " sampler was configured to use the " +
                                sampler.getSamplerType() + " sampler type" +
                                (sampler.getSamplerRatio() != null ? " with a ratio of " + sampler.getSamplerRatio() : "") +
                                (sampler.getSamplingTarget() != null ? " with a target of " + sampler.getSamplingTarget() : "") + ".");
        return sampler;
    }
}
