/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class AwsConfigImpl extends BaseConfig implements AwsConfig {

    public static final String ROOT = "cloud.aws";
    public static final String FARGATE_METADATA_ENDPOINT_PROXY_DISABLE = "fargate_metadata_endpoint_proxy_disable";
    private final boolean disableFargateMetadataEndpointProxy;

    public AwsConfigImpl(Map<String, Object> props) {
        super(props, ROOT);
        disableFargateMetadataEndpointProxy = getProperty(FARGATE_METADATA_ENDPOINT_PROXY_DISABLE, false);
    }

    @Override
    public boolean isFargateMetadataEndpointProxyDisabled() {
        return disableFargateMetadataEndpointProxy;
    }
}
