/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AwsConfigTest {

    @Test
    public void testDisableFargateMetadataEndpointProxy_ValueIsDefaultedFalse() {
        Map<String, Object> props = new HashMap<>();
        AwsConfig awsConfig = new AwsConfigImpl(props);

        assertFalse(awsConfig.isFargateMetadataEndpointProxyDisabled());
    }

    @Test
    public void testDisableFargateMetadataEndpointProxy_ValueIsSetToFalse() {
        Map<String, Object> props = new HashMap<>();
        props.put("fargate_metadata_endpoint_proxy_disable", false);
        AwsConfig awsConfig = new AwsConfigImpl(props);

        assertFalse(awsConfig.isFargateMetadataEndpointProxyDisabled());
    }

    @Test
    public void testDisableFargateMetadataEndpointProxy_ValueIsSetToTrue() {
        Map<String, Object> props = new HashMap<>();
        props.put("fargate_metadata_endpoint_proxy_disable", true);
        AwsConfig awsConfig = new AwsConfigImpl(props);

        assertTrue(awsConfig.isFargateMetadataEndpointProxyDisabled());
    }
}
