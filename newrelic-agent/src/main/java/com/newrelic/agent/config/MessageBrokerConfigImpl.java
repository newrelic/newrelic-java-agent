/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class MessageBrokerConfigImpl extends BaseConfig implements MessageBrokerConfig {

    public static final String ENABLED = "enabled";
    public static final String INSTANCE_REPORTING = "instance_reporting";
    public static final boolean INSTANCE_REPORTING_DEFAULT_ENABLED = true;

    public static final String PROPERTY_NAME = "message_broker_tracer";
    public static final String PROPERTY_ROOT = "newrelic.config." + PROPERTY_NAME + ".";
    public static final String DOT = ".";

    private final boolean isInstanceEnabled;

    public MessageBrokerConfigImpl(Map<String, Object> props) {
        super(props, PROPERTY_ROOT);
        BaseConfig instanceReportConfig = new BaseConfig(nestedProps(INSTANCE_REPORTING), PROPERTY_ROOT + INSTANCE_REPORTING + DOT);
        isInstanceEnabled = instanceReportConfig.getProperty(ENABLED, INSTANCE_REPORTING_DEFAULT_ENABLED);

    }

    @Override
    public boolean isInstanceReportingEnabled() {
        return isInstanceEnabled;
    }
}
