/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.application;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.ApplicationNamePriority;

public class SameOrHigherPriorityApplicationNamingPolicy extends AbstractApplicationNamingPolicy {

    private static final SameOrHigherPriorityApplicationNamingPolicy INSTANCE = new SameOrHigherPriorityApplicationNamingPolicy();

    private SameOrHigherPriorityApplicationNamingPolicy() {
    }

    @Override
    public boolean canSetApplicationName(Transaction transaction, ApplicationNamePriority priority) {
        PriorityApplicationName pan = transaction.getPriorityApplicationName();
        return priority.compareTo(pan.getPriority()) >= 0;
    }

    public static SameOrHigherPriorityApplicationNamingPolicy getInstance() {
        return INSTANCE;
    }

}
