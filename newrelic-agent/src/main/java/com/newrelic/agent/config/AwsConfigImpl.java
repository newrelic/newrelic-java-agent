/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class AwsConfigImpl extends BaseConfig implements AwsConfig {

    static final String AWS_ROOT = "aws.";
    
    public static final String FARGATE_METADATA_PROXY_BYPASS_ENABLED = "fargate_metadata_proxy_bypass_enabled";
    public static final boolean DEFAULT_FARGATE_METADATA_PROXY_BYPASS_ENABLED = false;

    public AwsConfigImpl(Map<String, Object> awsProps, String parentRoot) {
        super(awsProps, parentRoot + AWS_ROOT);

        isFargateMetadataProxyBypassEnabled();
    }

    @Override
    public boolean isFargateMetadataProxyBypassEnabled() {
        return getProperty(FARGATE_METADATA_PROXY_BYPASS_ENABLED, DEFAULT_FARGATE_METADATA_PROXY_BYPASS_ENABLED);
    }
}
