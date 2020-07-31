/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.scala;

import com.newrelic.agent.bridge.AgentBridge;
import scala.Function1;
import scala.PartialFunction;
import scala.Predef;
import scala.util.Try;

public class WrappedTry<T> extends Try<T> {

    public AgentBridge.TokenAndRefCount tokenAndRefCount;
    public Try<T> original;

    public WrappedTry(Try<T> original, AgentBridge.TokenAndRefCount tokenAndRefCount) {
        this.original = original;
        this.tokenAndRefCount = tokenAndRefCount;
    }

    @Override
    public boolean isFailure() {
        return original.isFailure();
    }

    @Override
    public boolean isSuccess() {
        return original.isSuccess();
    }

    @Override
    public T get() {
        return original.get();
    }

    @Override
    public <U> void foreach(Function1<T, U> f) {
        original.foreach(f);
    }

    @Override
    public <U> Try<U> flatMap(Function1<T, Try<U>> f) {
        return original.flatMap(f);
    }

    @Override
    public <U> Try<U> map(Function1<T, U> f) {
        return original.map(f);
    }

    @Override
    public Try<T> filter(Function1<T, Object> p) {
        return original.filter(p);
    }

    @Override
    public <U> Try<U> recoverWith(PartialFunction<Throwable, Try<U>> f) {
        return original.recoverWith(f);
    }

    @Override
    public <U> Try<U> recover(PartialFunction<Throwable, U> f) {
        return original.recover(f);
    }

    @Override
    public <U> Try<U> flatten(Predef.$less$colon$less<T, Try<U>> ev) {
        return original.flatten(ev);
    }

    @Override
    public Try<Throwable> failed() {
        return original.failed();
    }
}
