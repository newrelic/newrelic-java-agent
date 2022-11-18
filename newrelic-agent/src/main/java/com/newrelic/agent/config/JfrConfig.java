/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface JfrConfig extends EnableToggle {

    /**
     * Check if audit_logging is enabled for the JFR service.
     *
     * @return <code>true</code> is audit_logging is enabled for the JFR service is enabled, else <code>false</code>.
     */
    boolean auditLoggingEnabled();

    /**
     * Check if use_license_key is enabled for the JFR service.
     *
     * @return <code>true</code> is use_license_key is enabled for the JFR service is enabled, else <code>false</code>.
     */
    boolean useLicenseKey();
}
