/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.commands;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;

public class IntegerEmittingObservable extends HystrixObservableCommand<Integer> {

    private final Integer[] numbers;

    public IntegerEmittingObservable(Integer... numbers) {
        super(
                Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Sum-Command")).andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false)
                                                .withExecutionTimeoutEnabled(false)));
        this.numbers = numbers;
    }

    @Override
    protected Observable<Integer> construct() {
        return Observable.from(numbers);
    }
}
