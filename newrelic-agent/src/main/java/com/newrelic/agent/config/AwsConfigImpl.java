/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.util.Map;

public class AwsConfigImpl extends BaseConfig implements AwsConfig {

    public static final String ROOT = "cloud.aws";
    public static final String FARGATE_METADATA_ENDPOINT_PROXY_DISABLE = "fargate_metadata_endpoint_proxy_disable";
    private final boolean disableFargateMetadataEndpointProxy;

    public AwsConfigImpl(Map<String, Object> props, String parentRoot) {
        super(props, parentRoot + ROOT + ".");
        this.disableFargateMetadataEndpointProxy = getProperty(FARGATE_METADATA_ENDPOINT_PROXY_DISABLE, false);

        Agent.LOG.info("AWS Fargate Metadata Endpoint Proxy Disabled: " + disableFargateMetadataEndpointProxy);
    }

    @Override
    public boolean isFargateMetadataEndpointProxyDisabled() {
        return disableFargateMetadataEndpointProxy;
    }
}
