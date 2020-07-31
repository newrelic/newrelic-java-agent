/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class ExternalTracerConfigImpl extends BaseConfig implements ExternalTracerConfig {

    public static final String PROPERTY_NAME = "external_tracer";
    public static final String PROPERTY_ROOT = "newrelic.config." + PROPERTY_NAME + ".";

    public static final String EXCLUDE_REQUEST_URI = "exclude_request_uri";
    public static final boolean DEFAULT_EXCLUDE_REQUEST_URI = false;

    private final boolean excludeRequestUri;

    public ExternalTracerConfigImpl(Map<String, Object> props) {
        super(props, PROPERTY_ROOT);
        excludeRequestUri = getProperty(EXCLUDE_REQUEST_URI, DEFAULT_EXCLUDE_REQUEST_URI);
    }

    @Override
    public boolean excludeRequestUri() {
        return excludeRequestUri;
    }
}
