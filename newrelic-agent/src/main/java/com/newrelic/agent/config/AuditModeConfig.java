/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AuditModeConfig extends BaseConfig {

    public static final String ENABLED = "enabled";
    public static final boolean DEFAULT_ENABLED = false;
    public static final String ENDPOINTS = "endpoints";
    public static final String PROPERTY_NAME = "audit_mode";
    public static final String NESTED_PROPERTY_ROOT = "newrelic.config." + PROPERTY_NAME + ".";

    private boolean isEnabled;
    private Set<String> endpoints;

    /**
     * Legacy entry point where "audit_mode: false" is it's own single line config
     */
    public AuditModeConfig(boolean auditModeEnabled, boolean traceDataCalls) {
        super(null, null);
        this.isEnabled = auditModeEnabled || traceDataCalls;
    }

    /**
     * New entry point where "audit_mode:" is a nestable property.
     *
     * @param pProps nested prop values
     */
    public AuditModeConfig(Map<String, Object> pProps) {
        super(pProps, NESTED_PROPERTY_ROOT);
        this.isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        this.endpoints = new HashSet<>((getUniqueStrings(ENDPOINTS, COMMA_SEPARATOR)));
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Set<String> getEndpoints() {
        return endpoints;
    }

}
