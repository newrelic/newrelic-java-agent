package com.newrelic.agent.config;

import java.util.Map;

public class CloudConfigImpl extends BaseConfig implements CloudConfig {

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.cloud.";
    public static final String METADATA_PROXY_BYPASS_ENABLED = "metadata_proxy_bypass_enabled";
    public static final boolean DEFAULT_METADATA_PROXY_BYPASS_ENABLED = false;

    public CloudConfigImpl(Map<String, Object> cloudProps) {
        super(cloudProps, SYSTEM_PROPERTY_ROOT);
    }

    @Override
    public boolean isCloudMetadataProxyBypassEnabled() {
        return getProperty(METADATA_PROXY_BYPASS_ENABLED, DEFAULT_METADATA_PROXY_BYPASS_ENABLED);
    }
}
