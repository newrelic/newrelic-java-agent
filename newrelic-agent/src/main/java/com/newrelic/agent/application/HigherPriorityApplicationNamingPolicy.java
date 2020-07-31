/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.application;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.ApplicationNamePriority;

public class HigherPriorityApplicationNamingPolicy extends AbstractApplicationNamingPolicy {

    private static final HigherPriorityApplicationNamingPolicy INSTANCE = new HigherPriorityApplicationNamingPolicy();

    private HigherPriorityApplicationNamingPolicy() {
    }

    @Override
    public boolean canSetApplicationName(Transaction transaction, ApplicationNamePriority priority) {
        PriorityApplicationName pan = transaction.getPriorityApplicationName();
        return priority.compareTo(pan.getPriority()) > 0;
    }

    public static HigherPriorityApplicationNamingPolicy getInstance() {
        return INSTANCE;
    }

}
