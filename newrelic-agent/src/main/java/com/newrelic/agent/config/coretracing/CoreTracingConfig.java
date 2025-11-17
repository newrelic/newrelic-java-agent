/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.coretracing;

import com.newrelic.agent.config.BaseConfig;

import java.util.Map;

/**
 * This class provides the basic configuration structure for all three stanzas in which core tracing can be specified, eg:
 * <p>
 * distributed_tracing:
 *   sampler:
 *     root:
 *     ...
 *     full_granularity:
 *       root:
 *       ...
 *     partial_granularity:
 *       type:
 *       root:
 *       ...
 * <p>
 * The "sampler" and more specific "sampler.full_granularity" stanzas are equivalent. Stanza-specific configuration options are
 * provided in the subclasses.
 */
public class CoreTracingConfig extends BaseConfig {
    //granularity options
    public static final String BASE = "base";
    public static final String FULL_GRANULARITY = "full_granularity";
    public static final String PARTIAL_GRANULARITY = "partial_granularity";
    //enabled
    public static final String ENABLED = "enabled";
    // samplers
    public static final String ROOT = "root";
    public static final String REMOTE_PARENT_SAMPLED = "remote_parent_sampled";
    public static final String REMOTE_PARENT_NOT_SAMPLED = "remote_parent_not_sampled";

    private final boolean enabled;
    private SamplerConfig rootSampler;
    private SamplerConfig remoteParentSampledSampler;
    private SamplerConfig remoteParentNotSampledSampler;

    protected CoreTracingConfig(Map<String, Object> props, String completeSystemPropertyPrefix, boolean defaultEnabled) {
        super(props, completeSystemPropertyPrefix);
        this.enabled = getProperty(ENABLED, defaultEnabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    //These sampler-getters have to be lazy initialized due to subtle inheritance issues with FullGranularityConfig.
    public SamplerConfig getRootSampler(){
        if (rootSampler == null) {
            rootSampler = createSamplerConfig(ROOT);
        }
        return rootSampler;
    }

    public SamplerConfig getRemoteParentSampledSampler(){
        if (remoteParentSampledSampler == null) {
            remoteParentSampledSampler = createSamplerConfig(REMOTE_PARENT_SAMPLED);
        }
        return remoteParentSampledSampler;
    }

    public SamplerConfig getRemoteParentNotSampledSampler(){
        if (remoteParentNotSampledSampler == null) {
            remoteParentNotSampledSampler = createSamplerConfig(REMOTE_PARENT_NOT_SAMPLED);
        }
        return remoteParentNotSampledSampler;
    }

    public SamplerConfig getSamplerConfigForCase(String samplerCase) {
        switch (samplerCase) {
            case ROOT: return getRootSampler();
            case REMOTE_PARENT_SAMPLED: return getRemoteParentSampledSampler();
            case REMOTE_PARENT_NOT_SAMPLED: return getRemoteParentNotSampledSampler();
            default: return null;
        }
    }

    protected SamplerConfig createSamplerConfig(String samplerCase){
        return new SamplerConfig(samplerCase, getProperties(), systemPropertyPrefix);
    }
}