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

public class CommandCallsCommand extends HystrixCommand<Integer> {

    private int value;

    public CommandCallsCommand(Integer val) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Call-Command")).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false).withExecutionTimeoutEnabled(false)));
        this.value = val;
    }

    @Override
    protected Integer run() throws Exception {
        if (value > 1) {
            CommandCallsCommand command = new CommandCallsCommand(value - 1);
            return command.execute();
        } else {
            return 0;
        }
    }

}
