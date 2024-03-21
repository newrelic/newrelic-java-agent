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
    public static final String REGISTER_LINKING_METADATA_MBEAN = "linkingMetadataMBean";
    public static final String DISABLED_JMX_FRAMEWORKS = "disabled_jmx_frameworks";
    public static final String ENABLE_ITERATED_OBJECTNAME_KEYS = "enable_iterated_objectname_Keys";
    public static final boolean DEFAULT_REGISTER_LINKING_METADATA_MBEAN = false;
    public static final boolean DEFAULT_ENABLE_ITERATED_OBJECTNAME_KEYS = true;
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

    @Override
    public boolean registerLinkingMetadataMBean(){
        return getProperty(REGISTER_LINKING_METADATA_MBEAN, DEFAULT_REGISTER_LINKING_METADATA_MBEAN);
    }

    @Override
    public boolean enableIteratedObjectNameKeys() {
        return getProperty(ENABLE_ITERATED_OBJECTNAME_KEYS, DEFAULT_ENABLE_ITERATED_OBJECTNAME_KEYS);
    }
}
