package com.newrelic.agent.config;

import java.util.Map;

public class AwsConfigImpl extends BaseConfig implements AwsConfig {

    public static final String ROOT = "cloud.aws";
    public static final String DISABLE = "fargate_metadata_endpoint_proxy_disable";
    private final boolean disableFargateMetadataEndpointProxy;

    public AwsConfigImpl(Map<String, Object> props) {
        super(props, ROOT);
        disableFargateMetadataEndpointProxy = getProperty(DISABLE, false);
    }

    @Override
    public boolean isFargateMetadataEndpointProxyDisabled() {
        return disableFargateMetadataEndpointProxy;
    }
}
