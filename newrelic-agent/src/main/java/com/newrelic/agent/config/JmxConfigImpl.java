/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class JmxConfigImpl extends BaseConfig implements JmxConfig {

    public static final String ENABLED = "enabled";
    public static final String DISABLED_JMX_FRAMEWORKS = "disabled_jmx_frameworks";
    public static final Boolean DEFAULT_ENABLED = Boolean.TRUE;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.jmx.";

    private final boolean isEnabled;
    private final Collection<String> disabledJmxFrameworks;

    public JmxConfigImpl(Map<String, Object> pProps) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        disabledJmxFrameworks = getUniqueStrings(DISABLED_JMX_FRAMEWORKS, ",");
    }

    static JmxConfigImpl createJmxConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new JmxConfigImpl(settings);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Collection<String> getDisabledJmxFrameworks() {
        return disabledJmxFrameworks;
    }

}
