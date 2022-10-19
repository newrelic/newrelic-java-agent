/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ApplicationLoggingContextDataConfig extends BaseConfig {
    public static final String ROOT = "context_data";
    public static final String ENABLED = "enabled";
    public static final String INCLUDE = "include";
    public static final String EXCLUDE = "exclude";

    public static final boolean DEFAULT_ENABLED = false;

    private final boolean enabled;
    private final List<String> include;
    private final List<String> exclude;
    

    public ApplicationLoggingContextDataConfig(Map<String, Object> props, String parentRoot, boolean highSecurity) {
        super(props, parentRoot + ROOT + ".");
        enabled = !highSecurity && getProperty(ENABLED, DEFAULT_ENABLED);

        if (enabled) {
            include = getUniqueStrings(INCLUDE);
            exclude = getUniqueStrings(EXCLUDE);
        } else {
            include = Collections.emptyList();
            exclude = Collections.emptyList();
        }
    }

    public boolean getEnabled() {
        return enabled;
    }

    public List<String> getInclude() {
        return include;
    }

    public List<String> getExclude() {
        return exclude;
    }
}
