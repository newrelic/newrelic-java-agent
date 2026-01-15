package com.newrelic.agent.config;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CloudConfigImplTest {

    private static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.cloud.";

    @Test
    public void testGetAwsConfig_ReturnsNonNull() {
        CloudConfigImpl cloudConfig = new CloudConfigImpl(new HashMap<>(), SYSTEM_PROPERTY_ROOT);
        assertNotNull(cloudConfig.getAwsConfig());
    }

    @Test
    public void testGetAwsConfig_WithEmptyProps() {
        CloudConfigImpl cloudConfig = new CloudConfigImpl(Collections.emptyMap(), SYSTEM_PROPERTY_ROOT);
        AwsConfig awsConfig = cloudConfig.getAwsConfig();
        assertNotNull(awsConfig);
        assertFalse(awsConfig.isFargateMetadataProxyBypassEnabled());
    }

    @Test
    public void testGetAwsConfig_WithNestedAwsProperties() {
        Map<String, Object> awsProps = new HashMap<>();
        awsProps.put("fargate_metadata_proxy_bypass_enabled", true);

        Map<String, Object> props = new HashMap<>();
        props.put("aws", awsProps);

        CloudConfigImpl cloudConfig = new CloudConfigImpl(props, SYSTEM_PROPERTY_ROOT);
        assertTrue(cloudConfig.getAwsConfig().isFargateMetadataProxyBypassEnabled());
    }
}
