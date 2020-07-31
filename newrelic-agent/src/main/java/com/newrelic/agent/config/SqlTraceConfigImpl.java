/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.Map;

public final class SqlTraceConfigImpl extends BaseConfig implements SqlTraceConfig {

    public static final String ENABLED = "enabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String USE_LONGER_SQL_ID = "use_longer_sql_id";
    public static final boolean DEFAULT_USE_LONGER_SQL_ID = false;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.slow_sql.";

    private final boolean isEnabled;
    private final boolean isUsingLongerSqlId;

    private SqlTraceConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        isUsingLongerSqlId = getProperty(USE_LONGER_SQL_ID, DEFAULT_USE_LONGER_SQL_ID);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isUsingLongerSqlId() {
        return isUsingLongerSqlId;
    }

    @VisibleForTesting
    public static SqlTraceConfig createSqlTraceConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new SqlTraceConfigImpl(settings);
    }

}
