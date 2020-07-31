/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.config.UtilizationDataConfig;
import com.newrelic.agent.service.ServiceFactory;

/**
 * A holder for user-configure utilization values.
 */
public class UtilizationConfig {
    public static final UtilizationConfig EMPTY_DATA = new UtilizationConfig(null, null, null);

    /**
     * Create a utilization config from the agent config service.
     */
    public static UtilizationConfig createFromConfigService() {
        UtilizationDataConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getUtilizationDataConfig();
        return new UtilizationConfig(agentConfig.getBillingHostname(), agentConfig.getLogicalProcessorsConfig(), agentConfig.getTotalRamMibConfig());
    }

    private final String hostname;
    private final Integer logicalProcessors;
    private final Long totalRamMib;

    protected UtilizationConfig(String name, Integer processorCt, Long ram) {
        hostname = name;
        logicalProcessors = processorCt;
        totalRamMib = ram;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getLogicalProcessors() {
        return logicalProcessors;
    }

    public Long getTotalRamMib() {
        return totalRamMib;
    }

}
