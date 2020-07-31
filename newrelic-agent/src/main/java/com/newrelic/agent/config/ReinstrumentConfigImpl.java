/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

class ReinstrumentConfigImpl extends BaseConfig implements ReinstrumentConfig {

    public static final String ENABLED = "enabled";
    public static final Boolean DEFAULT_ENABLED = Boolean.TRUE;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.reinstrument.";
    public static final String ATTS_ENABLED = "attributes_enabled";
    public static final Boolean DEFAULT_ATTS_ENABLED = Boolean.FALSE;

    private final boolean isEnabled;
    private final boolean isAttsEnabled;

    public ReinstrumentConfigImpl(Map<String, Object> pProps) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        isAttsEnabled = getProperty(ATTS_ENABLED, DEFAULT_ATTS_ENABLED);
    }

    static ReinstrumentConfigImpl createReinstrumentConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new ReinstrumentConfigImpl(settings);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isAttributesEnabled() {
        return isAttsEnabled;
    }

}
