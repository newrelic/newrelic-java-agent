/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

class JfrConfigImpl extends BaseConfig implements JfrConfig {

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.jfr.";
    public static final String ENABLED = "enabled";
    public static final Boolean ENABLED_DEFAULT = Boolean.FALSE;
    public static final String AUDIT_LOGGING = "audit_logging";
    public static final Boolean AUDIT_LOGGING_DEFAULT = Boolean.FALSE;
    public static final Boolean USE_LICENSE_KEY_DEFAULT = Boolean.TRUE;
//    public static final String METRIC_INGEST_URI = "metric_ingest_uri";
//    public static final String METRIC_INGEST_URI_DEFAULT =
//            "https://metric-api.newrelic.com/metric/v1";
//    public static final String EVENT_INGEST_URI = "event_ingest_uri";
//    public static final String EVENT_INGEST_URI_DEFAULT =
//            "https://insights-collector.newrelic.com/v1/accounts/events";


    private final boolean isEnabled;

    public JfrConfigImpl(Map<String, Object> pProps) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, ENABLED_DEFAULT);
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
    public boolean auditLoggingEnabled() {
        return getProperty(AUDIT_LOGGING, AUDIT_LOGGING_DEFAULT);
    }

    @Override
    public boolean useLicenseKey() {
        return USE_LICENSE_KEY_DEFAULT;
    }

}
