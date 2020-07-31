/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface DatastoreConfig {

    /**
     * @return true if datastore_tracer.instance_reporting.enabled is enabled
     */
    boolean isInstanceReportingEnabled();

    /**
     * @return true if datastore_tracer.database_name_reporting.enabled is enabled
     */
    boolean isDatabaseNameReportingEnabled();

    /**
     * @return true if datastore_tracer.database_connection_metadata.enabled is enabled
     */
    boolean isDatabaseConnectionMetadataEnabled();

}
