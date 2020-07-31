/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.commands;

import rx.Observable;
import rx.Subscriber;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;

public class FailureObservableCommand extends HystrixObservableCommand<String> {
    private static final HystrixCommandGroupKey COMMAND_GROUP_KEY = HystrixCommandGroupKey.Factory.asKey("FailureObservableCommand");

    public FailureObservableCommand() {
        super(Setter.withGroupKey(COMMAND_GROUP_KEY).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withExecutionTimeoutEnabled(false).withCircuitBreakerEnabled(false)));
    }

    @Override
    protected Observable<String> construct() {
        throw new RuntimeException("Always fail");
    }

    @Override
    protected Observable<String> resumeWithFallback() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> s) {
                try {
                    s.onNext("We failed and went to fallback.");
                    s.onCompleted();
                } catch (Throwable e) {
                    s.onError(e);
                }
            }
        });
    }

}
