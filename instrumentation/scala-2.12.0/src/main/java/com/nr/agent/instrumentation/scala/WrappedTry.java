/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.scala;

import com.newrelic.agent.bridge.AgentBridge;
import scala.Function0;
import scala.Function1;
import scala.Option;
import scala.PartialFunction;
import scala.Predef;
import scala.util.Either;
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
    public <U> U getOrElse(Function0<U> var1) {
        return original.getOrElse(var1);
    }

    @Override
    public <U> Try<U> orElse(Function0<Try<U>> var1) {
        return original.orElse(var1);
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
    public <U> Try<U> collect(PartialFunction<T, U> pf) {
        return original.collect(pf);
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
    public Option<T> toOption() {
        return original.toOption();
    }

    @Override
    public <U> Try<U> flatten(Predef.$less$colon$less<T, Try<U>> ev) {
        return original.flatten(ev);
    }

    @Override
    public Try<Throwable> failed() {
        return original.failed();
    }

    @Override
    public <U> Try<U> transform(Function1<T, Try<U>> s, Function1<Throwable, Try<U>> f) {
        return original.transform(s, f);
    }

    @Override
    public Either<Throwable, T> toEither() {
        return original.toEither();
    }

    @Override
    public <U> U fold(Function1<Throwable, U> fa, Function1<T, U> fb) {
        return original.fold(fa, fb);
    }

    @Override
    public Object productElement(int n) {
        return original.productElement(n);
    }

    @Override
    public int productArity() {
        return original.productArity();
    }

    @Override
    public boolean canEqual(Object that) {
        return original.canEqual(that);
    }

    @Override
    public boolean equals(Object that) {
        return original.equals(that);
    }
}
