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

public class SleepDecrementCommand extends HystrixCommand<Integer> {

    private int value;

    public SleepDecrementCommand(int val) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Sleep-Command")).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false).withExecutionTimeoutEnabled(false)));
        this.value = val;
    }

    @Override
    protected Integer run() throws Exception {
        Thread.sleep(1);
        return value - 1;
    }

    @Override
    protected Integer getFallback() {
        return 0;
    }
}
