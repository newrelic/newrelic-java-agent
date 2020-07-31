/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.Harvestable;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transport.CollectorMethods;

public class InsightsHarvestableImpl extends Harvestable {

    InsightsHarvestableImpl(InsightsServiceImpl insightsService, String appName) {
        super(insightsService, appName);
    }

    @Override
    public String getEndpointMethodName() {
        return CollectorMethods.CUSTOM_EVENT_DATA;
    }

    @Override
    public int getMaxSamplesStored() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getInsightsConfig().getMaxSamplesStored();
    }
}
