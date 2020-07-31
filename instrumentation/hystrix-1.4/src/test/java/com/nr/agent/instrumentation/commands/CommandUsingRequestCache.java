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
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;

public class CommandUsingRequestCache extends HystrixCommand<Boolean> {

    private final int value;

    public CommandUsingRequestCache(int value) {
        super(
                Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RequestCache-Command")).andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter().withExecutionIsolationStrategy(
                                ExecutionIsolationStrategy.THREAD).withCircuitBreakerEnabled(false).withExecutionTimeoutEnabled(
                                false)));
        this.value = value;
    }

    @Override
    protected Boolean run() {
        return value == 0 || value % 2 == 0;
    }

    @Override
    public Boolean getFallback() {
        return false;
    }

    @Override
    protected String getCacheKey() {
        return String.valueOf(value);
    }

}
