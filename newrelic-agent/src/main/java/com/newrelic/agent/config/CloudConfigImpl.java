package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class CloudConfigImpl extends BaseConfig implements CloudConfig {

    static final String AWS_ROOT = "aws";
    static final String CLOUD_ROOT = "cloud.";

    private final AwsConfig awsConfig;

    public CloudConfigImpl(Map<String, Object> cloudProps, String parentRoot) {
        super(cloudProps, parentRoot + CLOUD_ROOT);

        Map<String, Object> awsProps = nestedProps(AWS_ROOT);
        if (awsProps == null) {
            awsProps = Collections.emptyMap();
        }
        this.awsConfig = new AwsConfigImpl(awsProps, parentRoot + CLOUD_ROOT);
    }

    @Override
    public AwsConfig getAwsConfig() {
        return awsConfig;
    }
}
