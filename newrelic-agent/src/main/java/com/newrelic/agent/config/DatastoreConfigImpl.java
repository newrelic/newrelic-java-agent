/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class DatastoreConfigImpl extends BaseConfig implements DatastoreConfig {

    public static final String ENABLED = "enabled";
    public static final String INSTANCE_REPORTING = "instance_reporting";
    public static final boolean INSTANCE_REPORTING_DEFAULT_ENABLED = true;
    public static final String DATABASE_NAME_REPORTING = "database_name_reporting";
    public static final boolean DATABASE_NAME_REPORTING_DEFAULT_ENABLED = true;

    public static final String DATABASE_CONNECTION_METADATA = "database_connection_metadata";
    public static final boolean DATABASE_CONNECTION_METADATA_ENABLED = true;

    public static final String PROPERTY_NAME = "datastore_tracer";
    public static final String PROPERTY_ROOT = "newrelic.config." + PROPERTY_NAME + ".";
    public static final String DOT = ".";

    private final boolean isInstanceEnabled;
    private final boolean isDatabaseNameEnabled;
    private final boolean isDatabaseConnectionMetadataEnabled;

    public DatastoreConfigImpl(Map<String, Object> props) {
        super(props, PROPERTY_ROOT);
        BaseConfig instanceReportConfig = new BaseConfig(nestedProps(INSTANCE_REPORTING), PROPERTY_ROOT + INSTANCE_REPORTING + DOT);
        isInstanceEnabled = instanceReportConfig.getProperty(ENABLED, INSTANCE_REPORTING_DEFAULT_ENABLED);

        BaseConfig databaseNameReportConfig = new BaseConfig(nestedProps(DATABASE_NAME_REPORTING), PROPERTY_ROOT + DATABASE_NAME_REPORTING + DOT);
        isDatabaseNameEnabled = databaseNameReportConfig.getProperty(ENABLED, DATABASE_NAME_REPORTING_DEFAULT_ENABLED);

        BaseConfig databaseConnectionMetadataConfig = new BaseConfig(nestedProps(DATABASE_CONNECTION_METADATA), PROPERTY_ROOT + DATABASE_CONNECTION_METADATA + DOT);
        isDatabaseConnectionMetadataEnabled = databaseConnectionMetadataConfig.getProperty(ENABLED, DATABASE_CONNECTION_METADATA_ENABLED);
    }

    @Override
    public boolean isInstanceReportingEnabled() {
        return isInstanceEnabled;
    }

    @Override
    public boolean isDatabaseNameReportingEnabled() {
        return isDatabaseNameEnabled;
    }

    @Override
    public boolean isDatabaseConnectionMetadataEnabled() {
        return isDatabaseConnectionMetadataEnabled;
    }
}
