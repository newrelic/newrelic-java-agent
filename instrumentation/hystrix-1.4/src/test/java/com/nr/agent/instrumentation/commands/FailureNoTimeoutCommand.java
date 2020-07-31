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

public class FailureNoTimeoutCommand extends HystrixCommand<String> {

    public FailureNoTimeoutCommand() {
        super(
                Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Failure-Command")).andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false).withExecutionTimeoutEnabled(
                                false)));
    }

    @Override
    protected String run() throws Exception {
        throw new Exception("The Failure command was called");
    }

    @Override
    protected String getFallback() {
        return "Failure: Failurecommand.";
    }

}
