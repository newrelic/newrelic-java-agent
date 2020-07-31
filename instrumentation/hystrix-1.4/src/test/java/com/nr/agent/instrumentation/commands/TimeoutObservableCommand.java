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

public class TimeoutObservableCommand extends HystrixObservableCommand<String> {

    private static final HystrixCommandGroupKey COMMAND_GROUP_KEY = HystrixCommandGroupKey.Factory.asKey("TimeoutObservableCommand");

    public TimeoutObservableCommand() {
        super(Setter.withGroupKey(COMMAND_GROUP_KEY).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(TimeoutCommand.TIMEOUT_MS)));
    }

    @Override
    protected Observable<String> construct() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> s) {
                try {
                    Thread.sleep(TimeoutCommand.TIMEOUT_MS * 2);
                    s.onNext(TimeoutCommand.FAILURE);
                    s.onCompleted();
                } catch (Throwable e) {
                    s.onError(e);
                }
            }
        });
    }

    @Override
    protected Observable<String> resumeWithFallback() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> s) {
                try {
                    if (isResponseTimedOut()) {
                        s.onNext(TimeoutCommand.SUCCESS);
                    } else {
                        s.onNext(TimeoutCommand.FAILURE);
                    }
                    s.onCompleted();
                } catch (Throwable e) {
                    s.onError(e);
                }
            }
        });
    }

}
