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

public class TransactionEventHarvestableImpl extends Harvestable {

    TransactionEventHarvestableImpl(TransactionEventsService transactionEventsService, String appName) {
        super(transactionEventsService, appName);
    }

    @Override
    public String getEndpointMethodName() {
        return CollectorMethods.ANALYTIC_EVENT_DATA;
    }

    @Override
    public int getMaxSamplesStored() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionEventsConfig().getMaxSamplesStored();
    }
}
