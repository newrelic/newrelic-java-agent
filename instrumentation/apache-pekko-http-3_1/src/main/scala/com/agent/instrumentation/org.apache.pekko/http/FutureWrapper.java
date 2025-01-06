/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weaver;
import scala.$less$colon$less;
import scala.Function1;
import scala.Function2;
import scala.Option;
import scala.PartialFunction;
import scala.Tuple2;
import scala.concurrent.Awaitable;
import scala.concurrent.CanAwait;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.reflect.ClassTag;
import scala.util.Try;

import java.util.concurrent.TimeoutException;

public class FutureWrapper<T> implements Future<T> {

    private final Future<T> original;

    public FutureWrapper(Future<T> original) {
        this.original = original;
    }

    @Override
    public <U> void onComplete(Function1<Try<T>, U> f, ExecutionContext executor) {
        try {
            f = new Function1Wrapper<>(f, NewRelic.getAgent().getTransaction().getToken());
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
        original.onComplete(f, executor);
    }

    @Override
    public boolean isCompleted() {
        return original.isCompleted();
    }

    @Override
    public Option<Try<T>> value() {
        return original.value();
    }

    @Override
    public Future<Throwable> failed() {
        return original.failed();
    }

    @Override
    public <U> void foreach(Function1<T, U> f, ExecutionContext executor) {
        original.foreach(f, executor);
    }

    @Override
    public <S> Future<S> transform(Function1<T, S> s, Function1<Throwable, Throwable> f, ExecutionContext executor) {
        return original.transform(s, f, executor);
    }

    @Override
    public <S> Future<S> transform(Function1<Try<T>, Try<S>> f, ExecutionContext executor) {
        return original.transform(f, executor);
    }

    @Override
    public <S> Future<S> transformWith(Function1<Try<T>, Future<S>> f, ExecutionContext executor) {
        return original.transformWith(f, executor);
    }

    @Override
    public <S> Future<S> map(Function1<T, S> f, ExecutionContext executor) {
        return original.map(f, executor);
    }

    @Override
    public <S> Future<S> flatMap(Function1<T, Future<S>> f, ExecutionContext executor) {
        return original.flatMap(f, executor);
    }

    @Override
    public <S> Future<S> flatten($less$colon$less<T, Future<S>> ev) {
        return original.flatten(ev);
    }

    @Override
    public Future<T> filter(Function1<T, Object> p, ExecutionContext executor) {
        return original.filter(p, executor);
    }

    @Override
    public Future<T> withFilter(Function1<T, Object> p, ExecutionContext executor) {
        return original.withFilter(p, executor);
    }

    @Override
    public <S> Future<S> collect(PartialFunction<T, S> pf, ExecutionContext executor) {
        return original.collect(pf, executor);
    }

    @Override
    public <U> Future<U> recover(PartialFunction<Throwable, U> pf, ExecutionContext executor) {
        return original.recover(pf, executor);
    }

    @Override
    public <U> Future<U> recoverWith(PartialFunction<Throwable, Future<U>> pf, ExecutionContext executor) {
        return original.recoverWith(pf, executor);
    }

    @Override
    public <U> Future<Tuple2<T, U>> zip(Future<U> that) {
        return original.zip(that);
    }

    @Override
    public <U, R> Future<R> zipWith(Future<U> that, Function2<T, U, R> f, ExecutionContext executor) {
        return original.zipWith(that, f, executor);
    }

    @Override
    public <U> Future<U> fallbackTo(Future<U> that) {
        return original.fallbackTo(that);
    }

    @Override
    public <S> Future<S> mapTo(ClassTag<S> tag) {
        return original.mapTo(tag);
    }

    @Override
    public <U> Future<T> andThen(PartialFunction<Try<T>, U> pf, ExecutionContext executor) {
        return original.andThen(pf, executor);
    }

    @Override
    public Awaitable<T> ready(Duration atMost, CanAwait permit) throws InterruptedException, TimeoutException {
        return original.ready(atMost, permit);
    }

    @Override
    public T result(Duration atMost, CanAwait permit) throws TimeoutException, InterruptedException {
        return original.result(atMost, permit);
    }
}
