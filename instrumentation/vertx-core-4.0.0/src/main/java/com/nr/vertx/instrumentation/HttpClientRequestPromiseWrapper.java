/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.vertx.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import io.netty.util.concurrent.Future;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.future.Listener;
import io.vertx.core.impl.future.PromiseInternal;

import java.util.function.Function;

import static com.nr.vertx.instrumentation.VertxCoreUtil.END;
import static com.nr.vertx.instrumentation.VertxCoreUtil.VERTX_CLIENT;

public class HttpClientRequestPromiseWrapper implements PromiseInternal<HttpClientRequest> {

    private final PromiseInternal<HttpClientRequest> original;

    private Token token;

    public HttpClientRequestPromiseWrapper(PromiseInternal<HttpClientRequest> original, Token token) {
        this.original = original;
        this.token = token;
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void complete(HttpClientRequest req) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        original.complete(req);
    }


    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean tryFail(Throwable t) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (t.toString().contains("UnknownHostException") && txn != null) {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment(VERTX_CLIENT, END);
            VertxCoreUtil.reportUnknownHost(segment);
            segment.end();
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            AgentBridge.getAgent().getTransaction(false).expireAllTokens();
        }

        return original.tryFail(t);
    }

    @Override
    public boolean tryComplete(HttpClientRequest result) {
        return original.tryComplete(result);
    }

    @Override
    public io.vertx.core.Future<HttpClientRequest> future() {
        return original.future();
    }

    public ContextInternal context() {
        return original.context();
    }

    @Override
    public void addListener(Listener<HttpClientRequest> listener) {
        original.addListener(listener);
    }

    @Override
    public void operationComplete(Future<HttpClientRequest> future) throws Exception {
        original.operationComplete(future);
    }

    @Override
    public boolean isComplete() {
        return original.isComplete();
    }

    @Override
    public io.vertx.core.Future<HttpClientRequest> onComplete(Handler<AsyncResult<HttpClientRequest>> handler) {
        return original.onComplete(handler);
    }

    @Override
    public HttpClientRequest result() {
        return original.result();
    }

    @Override
    public Throwable cause() {
        return original.cause();
    }

    @Override
    public boolean succeeded() {
        return original.succeeded();
    }

    @Override
    public boolean failed() {
        return original.failed();
    }

    @Override
    public <U> io.vertx.core.Future<U> compose(Function<HttpClientRequest, io.vertx.core.Future<U>> successMapper,
            Function<Throwable, io.vertx.core.Future<U>> failureMapper) {
        return original.compose(successMapper, failureMapper);
    }

    @Override
    public <U> io.vertx.core.Future<U> eventually(Function<Void, io.vertx.core.Future<U>> mapper) {
        return original.eventually(mapper);
    }

    @Override
    public <U> io.vertx.core.Future<U> map(Function<HttpClientRequest, U> mapper) {
        return original.map(mapper);
    }

    @Override
    public <V> io.vertx.core.Future<V> map(V value) {
        return original.map(value);
    }

    @Override
    public io.vertx.core.Future<HttpClientRequest> otherwise(Function<Throwable, HttpClientRequest> mapper) {
        return original.otherwise(mapper);
    }

    @Override
    public io.vertx.core.Future<HttpClientRequest> otherwise(HttpClientRequest value) {
        return original.otherwise(value);
    }
}
