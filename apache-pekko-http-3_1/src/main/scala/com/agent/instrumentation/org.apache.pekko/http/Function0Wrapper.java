/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http;

import scala.Function0;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction0;

public class Function0Wrapper<T> extends AbstractFunction0<Future<T>> {

    private final Function0<Future<T>> original;

    public Function0Wrapper(Function0<Future<T>> original) {
        this.original = original;
    }

    @Override
    public Future<T> apply() {
        Future<T> result = original.apply();
        return new FutureWrapper<>(result);
    }

}
