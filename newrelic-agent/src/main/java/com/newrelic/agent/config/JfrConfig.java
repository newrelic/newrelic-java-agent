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

    /**
     * If true, the JFR host name will be set to the value in the <code>process_host.display_name</code> config.
     * Note that if the <code>process_host.display_name</code> value is empty/null, the default hostname
     * resolution logic will be used.
     *
     * @return <code>true</code> if the configured display name should be used for the JFR host name, else <code>false</code>.
     */
    boolean useDisplayName();
}
