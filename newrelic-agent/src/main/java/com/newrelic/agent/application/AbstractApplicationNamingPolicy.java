/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.application;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.ApplicationNamePriority;

public abstract class AbstractApplicationNamingPolicy implements ApplicationNamingPolicy {

    @Override
    public abstract boolean canSetApplicationName(Transaction transaction, ApplicationNamePriority priority);

}
