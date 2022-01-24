/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.logging;

import com.newrelic.agent.Harvestable;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transport.CollectorMethods;

public class LogSenderHarvestableImpl extends Harvestable {

    LogSenderHarvestableImpl(LogSenderServiceImpl logSenderService, String appName) {
        super(logSenderService, appName);
    }

    /**
     * Agent endpoint to send log data to.
     *
     * @return String representing the endpoint name
     */
    @Override
    public String getEndpointMethodName() {
        return CollectorMethods.LOG_EVENT_DATA;
    }

    /**
     * Number of log sender events that can be stored.
     *
     * @return int for max samples stored
     */
    @Override
    public int getMaxSamplesStored() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getLogSenderConfig().getMaxSamplesStored();
    }
}
