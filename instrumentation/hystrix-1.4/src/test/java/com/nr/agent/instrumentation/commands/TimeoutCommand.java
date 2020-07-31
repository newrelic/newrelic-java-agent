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

public class TimeoutCommand extends HystrixCommand<String> {
    public static final int TIMEOUT_MS = 100;
    public static final String SUCCESS = "Success - Command Timed Out - YAY!";
    public static final String FAILURE = "Failure - Command Should Have Timed Out - BOO!";

    public TimeoutCommand() {
        super(
                Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Failure-Command")).andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false).withExecutionTimeoutEnabled(
                                true).withExecutionTimeoutInMilliseconds(TIMEOUT_MS)));
    }

    @Override
    protected String run() throws Exception {
        Thread.sleep(TIMEOUT_MS * 2);
        return FAILURE;
    }

    @Override
    protected String getFallback() {
        if (isResponseTimedOut()) {
            return SUCCESS;
        }
        return FAILURE;
    }

}
