/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.netflix.hystrix.strategy.concurrency;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;

/**
 * This class is used internally by a scheduler for work that needs to be executed. The {@link #call()} method is
 * executed when the work is started by the scheduler; we start a transaction here.
 */
@Weave
public abstract class HystrixContexSchedulerAction {

    @Trace(dispatcher = true)
    public abstract void call();

}
