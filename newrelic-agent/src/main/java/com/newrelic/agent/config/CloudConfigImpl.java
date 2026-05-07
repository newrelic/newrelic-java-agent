/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class CloudConfigImpl extends BaseConfig implements CloudConfig {

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.cloud.";
    public static final String METADATA_BYPASS_PROXY = "metadata_bypass_proxy";
    public static final boolean DEFAULT_METADATA_BYPASS_PROXY = false;

    public CloudConfigImpl(Map<String, Object> cloudProps) {
        super(cloudProps, SYSTEM_PROPERTY_ROOT);
    }

    @Override
    public boolean isCloudMetadataBypassProxyEnabled() {
        return getProperty(METADATA_BYPASS_PROXY, DEFAULT_METADATA_BYPASS_PROXY);
    }
}
