/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.Harvestable;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transport.CollectorMethods;

public class ErrorHarvestableImpl extends Harvestable {

    ErrorHarvestableImpl(ErrorServiceImpl errorService, String appName) {
        super(errorService, appName);
    }

    @Override
    public String getEndpointMethodName() {
        return CollectorMethods.ERROR_EVENT_DATA;
    }

    @Override
    public int getMaxSamplesStored() {
        return ServiceFactory.getConfigService().getErrorCollectorConfig(this.getAppName()).getMaxSamplesStored();
    }
}
