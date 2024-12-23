/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface JfrConfig {

    /**
     * Check if the JFR service is enabled.
     *
     * @return <code>true</code> if the JFR service is enabled, else <code>false</code>.
     */
    boolean isEnabled();

    /**
     * The harvest interval to be used by the JfrController to trigger a harvest.
     * @return the amount of seconds between two JFR data harvests
     */
    Integer getHarvestInterval();

    /**
     * The size of the queue that holds JFR's RecordedEvents to be processed in each harvest.
     * @return the number of events to process
     */
    Integer getQueueSize();

    /**
     * Set the JFR service enabled flag
     *
     * @param enabled the desired enabled flag value
     */
    void setEnabled(boolean enabled);

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
