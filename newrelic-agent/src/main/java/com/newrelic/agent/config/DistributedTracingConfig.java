/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.tracing.samplers.Sampler;
import com.newrelic.api.agent.NewRelic;

import java.util.Map;
import java.util.logging.Level;

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
    public static final String SAMPLER = "sampler";
    public static final String ADAPTIVE_SAMPLING_TARGET = "adaptive_sampling_target";  // see below
    public static final String REMOTE_PARENT_SAMPLED = "remote_parent_sampled";
    public static final String REMOTE_PARENT_NOT_SAMPLED = "remote_parent_not_sampled";

    //public setting values
    public static final Integer DEFAULT_ADAPTIVE_SAMPLING_TARGET = 120;
    public static final Integer DEFAULT_ADAPTIVE_SAMPLING_PERIOD = 60;
    // note: there is no special logic for these yet, as they are just the fallback if the other values don't exist
    // if we have to add special logic, we should treat one as an alias of the other
    public static final String SAMPLE_DEFAULT = "default"; // same as 'adaptive_sampling'
    public static final String SAMPLE_ADAPTIVE_SAMPLING = "adaptive_sampling";
    public static final String SAMPLE_ALWAYS_ON = "always_on";
    public static final String SAMPLE_ALWAYS_OFF = "always_off";// same as 'default'

    private final boolean enabled;
    private final String trustedAccountKey;
    private final String accountId;
    private final String primaryApplicationId;
    private final boolean includeNewRelicHeader;
    private final String remoteParentSampled;
    private final String remoteParentNotSampled;

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

        BaseConfig samplerConfig = new BaseConfig(nestedProps(SAMPLER), SYSTEM_PROPERTY_ROOT+SAMPLER+".");
        this.adaptiveSamplingTarget = samplerConfig.getProperty(ADAPTIVE_SAMPLING_TARGET, DEFAULT_ADAPTIVE_SAMPLING_TARGET);
        this.remoteParentSampled = samplerConfig.getProperty(REMOTE_PARENT_SAMPLED, SAMPLE_DEFAULT);
        this.remoteParentNotSampled = samplerConfig.getProperty(REMOTE_PARENT_NOT_SAMPLED, SAMPLE_DEFAULT);
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

    public String getRemoteParentSampled() {
        return remoteParentSampled;
    }

    public String getRemoteParentNotSampled() {
        return remoteParentNotSampled;
    }

    public Integer getAdaptiveSamplingTarget() {
        return adaptiveSamplingTarget;
    }
}
