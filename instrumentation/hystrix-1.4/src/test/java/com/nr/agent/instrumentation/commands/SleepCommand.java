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

/**
 * Sleep command with circuit breaker and timeout disabled.
 */
public class SleepCommand extends HystrixCommand<String> {

    private long timeToSleep;

    public SleepCommand(long sleepTime) {

        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Sleep-Command")).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false).withExecutionTimeoutEnabled(false)));
        timeToSleep = sleepTime;

    }

    @Override
    protected String run() throws Exception {
        Thread.sleep(timeToSleep);
        return "SleepCommand: Slept for " + timeToSleep;
    }

    @Override
    protected String getFallback() {
        return "Failure: SleepCommand: " + timeToSleep;
    }

}
