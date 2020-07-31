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
 * Circuit Breaker and Timeout is disabled.
 */
public class HelloWorldCommand extends HystrixCommand<String> {

    public HelloWorldCommand() {
        super(
                Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("HelloWorld-Command")).andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false).withExecutionTimeoutEnabled(
                                false)));
    }

    @Override
    protected String run() throws Exception {
        return "Hello World";
    }

    @Override
    public String getFallback() {
        return "Hello failure";
    }

}
