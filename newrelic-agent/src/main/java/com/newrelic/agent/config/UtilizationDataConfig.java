/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.util.Map;
import java.util.logging.Level;

/**
 * Config section for utilization data.
 */
public class UtilizationDataConfig extends BaseConfig {

    public static final String PROPERTY_NAME = "utilization";
    public static final String PROPERTY_ROOT = "newrelic.config." + PROPERTY_NAME + ".";

    private static final String LOGICAL_PROC_ENV_KEY = "NEW_RELIC_UTILIZATION_LOGICAL_PROCESSORS";
    private static final String LOGICAL_PROCESSORS = "logical_processors";

    private static final String TOTAL_RAM_ENV_KEY = "NEW_RELIC_UTILIZATION_TOTAL_RAM_MIB";
    private static final String TOTAL_RAM_MIB = "total_ram_mib";

    private static final String HOSTNAME_ENV_KEY = "NEW_RELIC_UTILIZATION_BILLING_HOSTNAME";
    private static final String BILLING_HOSTNAME = "billing_hostname";

    private Long totalRamMibConfig;
    private Integer logicalProcessorsConfig;
    private String billingHostname;

    private String parseConfigVal(String key) {
        if (getProperty(key) != null) {
            return getProperty(key).toString();
        }
        return null;
    }

    public UtilizationDataConfig(Map<String, Object> props) throws NumberFormatException {
        super(props, PROPERTY_ROOT);
        try {
            totalRamMibConfig = parseConfigVal(TOTAL_RAM_MIB) == null ? null : new Long(parseConfigVal(TOTAL_RAM_MIB));
        } catch (NumberFormatException e) {
            Agent.LOG.log(Level.WARNING, "Number expected total_ram_mib configuration {0}", e);
        }
        try {
            logicalProcessorsConfig = parseConfigVal(LOGICAL_PROCESSORS) == null ? null : new Integer(parseConfigVal(LOGICAL_PROCESSORS));
        } catch (NumberFormatException e) {
            Agent.LOG.log(Level.WARNING, "Number expected for logical_processors configuration {0}", e);
        }
        billingHostname = getProperty(BILLING_HOSTNAME);
    }

    @Override
    protected String getSystemPropertyKey(String key) {
        return key;
    }

    public Long getTotalRamMibConfig() {
        final Object envVal = getPropertyFromSystemEnvironment(TOTAL_RAM_ENV_KEY, null);
        if (envVal != null) {
            try {
                return Long.parseLong(String.valueOf(envVal));
            } catch (NumberFormatException nfe) {
                Agent.LOG.log(Level.WARNING, "Sys prop {0}={1} is not a valid number", TOTAL_RAM_ENV_KEY, envVal);
            }
        }
        return totalRamMibConfig;
    }

    public Integer getLogicalProcessorsConfig() {
        final Object envVal = getPropertyFromSystemEnvironment(LOGICAL_PROC_ENV_KEY, null);
        if (envVal != null) {
            try {
                return Integer.parseInt(String.valueOf(envVal));
            } catch (NumberFormatException nfe) {
                Agent.LOG.log(Level.WARNING, "Sys prop {0}={1} is not a valid number", LOGICAL_PROC_ENV_KEY, envVal);
            }
        }
        return logicalProcessorsConfig;
    }

    public String getBillingHostname() {
        final Object envVal = getPropertyFromSystemEnvironment(HOSTNAME_ENV_KEY, null);
        if (envVal != null) {
            return String.valueOf(envVal);
        } else {
            return billingHostname;
        }
    }
}
