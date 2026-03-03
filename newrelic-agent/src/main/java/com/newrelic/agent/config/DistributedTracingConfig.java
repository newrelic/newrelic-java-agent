/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.config.coretracing.BaseSamplerCoreTracingConfig;
import com.newrelic.agent.config.coretracing.CoreTracingConfig;
import com.newrelic.agent.config.coretracing.PartialGranularityConfig;
import com.newrelic.agent.config.coretracing.SamplerConfig;

import java.util.Map;

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
    public static final String FULL_GRANULARITY = "full_granularity";
    public static final String PARTIAL_GRANULARITY = "partial_granularity";

    private final boolean enabled;
    private final String trustedAccountKey;
    private final String accountId;
    private final String primaryApplicationId;
    private final boolean includeNewRelicHeader;
    // the top-level, legacy sampler configuration
    private final BaseSamplerCoreTracingConfig baseSamplerConfig;

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
        this.baseSamplerConfig = new BaseSamplerCoreTracingConfig(nestedProps("sampler"), SYSTEM_PROPERTY_ROOT);
        this.adaptiveSamplingTarget = this.baseSamplerConfig.getSharedAdaptiveSamplingTarget();
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

    public int getAdaptiveSamplingTarget() {
        return adaptiveSamplingTarget;
    }

    public CoreTracingConfig getFullGranularityConfig() {
        return baseSamplerConfig.getFullGranularityConfig();
    }

    public PartialGranularityConfig getPartialGranularityConfig() {
        return baseSamplerConfig.getPartialGranularityConfig();
    }

    @Deprecated
    public SamplerConfig getRootSamplerConfig() {
        return getFullGranularityConfig().getRootSampler();
    }

    @Deprecated
    public SamplerConfig getRemoteParentSampledSamplerConfig() {
        return getFullGranularityConfig().getRemoteParentSampledSampler();
    }

    @Deprecated
    public SamplerConfig getRemoteParentNotSampledSamplerConfig() {
        return getFullGranularityConfig().getRemoteParentNotSampledSampler();
    }
}
