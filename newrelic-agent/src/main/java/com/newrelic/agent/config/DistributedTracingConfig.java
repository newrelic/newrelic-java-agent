/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class DistributedTracingConfig extends BaseConfig {

    private static final boolean DEFAULT_DISTRIBUTED_TRACING = true;
    private static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.distributed_tracing.";

    public static final String ENABLED = "enabled";
    public static final String TRUSTED_ACCOUNT_KEY = "trusted_account_key";
    public static final String ACCOUNT_ID = "account_id";
    public static final String PRIMARY_APPLICATION_ID = "primary_application_id";
    public static final String DISTRIBUTED_TRACING_ENABLED = SYSTEM_PROPERTY_ROOT + ENABLED;
    public static final String ENABLED_ENV_KEY = "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED";
    public static final String EXCLUDE_NEWRELIC_HEADER = "exclude_newrelic_header";
    public static final String SAMPLER = "sampler";
    public static final String REMOTE_PARENT_SAMPLED = "remote_parent_sampled";
    public static final String REMOTE_PARENT_NOT_SAMPLED = "remote_parent_not_sampled";
    public static final String ROOT = "root";

    private final boolean enabled;
    private final String trustedAccountKey;
    private final String accountId;
    private final String primaryApplicationId;
    private final boolean includeNewRelicHeader;
    private final SamplerConfig remoteParentSampledConfig;
    private final SamplerConfig remoteParentNotSampledConfig;
    private final SamplerConfig rootSamplerConfig;

    DistributedTracingConfig(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        this.enabled = getProperty(ENABLED, DEFAULT_DISTRIBUTED_TRACING);
        this.trustedAccountKey = getProperty(TRUSTED_ACCOUNT_KEY);
        this.accountId = getProperty(ACCOUNT_ID);
        this.primaryApplicationId = getProperty(PRIMARY_APPLICATION_ID);
        this.includeNewRelicHeader = !getProperty(EXCLUDE_NEWRELIC_HEADER, false);
        this.remoteParentSampledConfig = initSamplerConfig(REMOTE_PARENT_SAMPLED);
        this.remoteParentNotSampledConfig = initSamplerConfig(REMOTE_PARENT_NOT_SAMPLED);
        this.rootSamplerConfig = initSamplerConfig(ROOT);
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

    public SamplerConfig getRemoteParentSampledSamplerConfig() {
        return remoteParentSampledConfig;
    }

    public SamplerConfig getRemoteParentNotSampledSamplerConfig() {
        return remoteParentNotSampledConfig;
    }

    public SamplerConfig getRootSamplerConfig(){
        return rootSamplerConfig;
    }

    private SamplerConfig initSamplerConfig(String subsection) {
        String prefix = this.systemPropertyPrefix + subsection + ".";
        Object val = getProperty(subsection);
        return SamplerConfig.createSamplerConfig(val, prefix);
    }
}
