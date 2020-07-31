/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.netflix.hystrix;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

import java.util.concurrent.Future;

/**
 * Work for multiple response can all be executed in one batch. For now we are not linking 
 * the batch command to any of the input response transactions. It will be its own separate 
 * background transaction.
 */
@Weave(type = MatchType.BaseClass)
public abstract class HystrixCollapser<BatchReturnType, ResponseType, RequestArgumentType> {

    @Trace
    public abstract Future<ResponseType> queue();
}
