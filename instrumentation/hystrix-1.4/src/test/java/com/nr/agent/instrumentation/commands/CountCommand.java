/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.commands;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

public class CountCommand extends HystrixCommand<Integer> {

    public CountCommand() {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Count-Command")).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false).withExecutionTimeoutEnabled(false)));

    }

    @Override
    protected Integer run() throws Exception {
        return 5;
    }

    @Override
    public Integer getFallback() {
        return 1;
    }

}
