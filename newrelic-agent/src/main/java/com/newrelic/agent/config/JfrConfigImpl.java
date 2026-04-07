/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.jfr.daemon.DaemonConfig;

import java.util.Collections;
import java.util.Map;

class JfrConfigImpl extends BaseConfig implements JfrConfig {

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.jfr.";
    public static final String ENABLED = "enabled";
    public static final Boolean ENABLED_DEFAULT = Boolean.FALSE;
    public static final String AUDIT_LOGGING = "audit_logging";
    public static final Boolean AUDIT_LOGGING_DEFAULT = Boolean.FALSE;
    public static final Boolean USE_LICENSE_KEY_DEFAULT = Boolean.TRUE;
    public static final String HARVEST_INTERVAL = "harvest_interval";   //In seconds
    public static final String QUEUE_SIZE = "queue_size";

    private boolean isEnabled;
    private final Integer harvestInterval;
    private final Integer queueSize;

    public JfrConfigImpl(Map<String, Object> pProps) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, ENABLED_DEFAULT);
        harvestInterval = getProperty(HARVEST_INTERVAL, DaemonConfig.DEFAULT_HARVEST_INTERVAL);
        queueSize = getProperty(QUEUE_SIZE, DaemonConfig.DEFAULT_QUEUE_SIZE);
    }

    static JfrConfigImpl createJfrConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new JfrConfigImpl(settings);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Integer getHarvestInterval() {
        return harvestInterval;
    }

    @Override
    public Integer getQueueSize() {
        return queueSize;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    @Override
    public boolean auditLoggingEnabled() {
        return getProperty(AUDIT_LOGGING, AUDIT_LOGGING_DEFAULT);
    }

    @Override
    public boolean useLicenseKey() {
        return USE_LICENSE_KEY_DEFAULT;
    }
}
