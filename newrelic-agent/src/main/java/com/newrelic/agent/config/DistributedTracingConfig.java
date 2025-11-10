/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

import static com.newrelic.agent.config.SamplerConfig.REMOTE_PARENT_NOT_SAMPLED;
import static com.newrelic.agent.config.SamplerConfig.REMOTE_PARENT_SAMPLED;
import static com.newrelic.agent.config.SamplerConfig.ROOT;

public class DistributedTracingConfig extends BaseConfig {

    private static final boolean DEFAULT_DISTRIBUTED_TRACING = true;
    private static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.distributed_tracing.";

    //public setting names
    public static final String ENABLED = "enabled";
    public static final String TRUSTED_ACCOUNT_KEY = "trusted_account_key";
    public static final String ACCOUNT_ID = "account_id";
    public static final String PRIMARY_APPLICATION_ID = "primary_application_id";
    public static final String DISTRIBUTED_TRACING_ENABLED = SYSTEM_PROPERTY_ROOT + ENABLED;
    public static final String ENABLED_ENV_KEY = "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED";
    public static final String EXCLUDE_NEWRELIC_HEADER = "exclude_newrelic_header";

    private final boolean enabled;
    private final String trustedAccountKey;
    private final String accountId;
    private final String primaryApplicationId;
    private final boolean includeNewRelicHeader;

    private final SamplerConfig rootSamplerConfig;
    private final SamplerConfig remoteParentSampledConfig;
    private final SamplerConfig remoteParentNotSampledConfig;

    // will be passed up on connect and come back down as just 'sampling_target'
    // which will get assigned to transaction_events.target_samples_stored
    // so, even though it's not directly used in code here, it is necessary
    private final Integer adaptiveSamplingTarget;

    DistributedTracingConfig(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        this.enabled = getProperty(ENABLED, DEFAULT_DISTRIBUTED_TRACING);
        this.trustedAccountKey = getProperty(TRUSTED_ACCOUNT_KEY);
        this.accountId = getProperty(ACCOUNT_ID);
        this.primaryApplicationId = getProperty(PRIMARY_APPLICATION_ID);
        this.includeNewRelicHeader = !getProperty(EXCLUDE_NEWRELIC_HEADER, false);

        this.rootSamplerConfig = createSamplerConfig(ROOT);
        this.remoteParentSampledConfig = createSamplerConfig(REMOTE_PARENT_SAMPLED);
        this.remoteParentNotSampledConfig = createSamplerConfig(REMOTE_PARENT_NOT_SAMPLED);

        // The adaptive_sampling_target can be retrieved from any of the SamplerConfigs as
        // they'll all return the same value. Here we use the root sampler SamplerConfig instance.
        this.adaptiveSamplingTarget = this.rootSamplerConfig.getAdaptiveSamplingTarget();
    }

    public String getTrustedAccountKey() {
        return trustedAccountKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getPrimaryApplicationId() {
        return primaryApplicationId;
    }

    public String getAccountId() {
        return accountId;
    }

    public boolean isIncludeNewRelicHeader() {
        return includeNewRelicHeader;
    }

    public Integer getAdaptiveSamplingTarget() {
        return adaptiveSamplingTarget;
    }

    private SamplerConfig createSamplerConfig(String samplerType) {
        Map<String, Object> samplerProps = getProperty(SamplerConfig.SAMPLER_CONFIG_ROOT, Collections.emptyMap());
        return new SamplerConfig(samplerType, samplerProps, systemPropertyPrefix);
    }

    public SamplerConfig getRootSamplerConfig() {
        return rootSamplerConfig;
    }

    public SamplerConfig getRemoteParentSampledSamplerConfig() {
        return remoteParentSampledConfig;
    }

    public SamplerConfig getRemoteParentNotSampledSamplerConfig() {
        return remoteParentNotSampledConfig;
    }
}
