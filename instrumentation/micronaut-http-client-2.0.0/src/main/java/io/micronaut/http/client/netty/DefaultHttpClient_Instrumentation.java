/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.client.netty;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.micronaut.http.client.MicronautHeaders;
import com.nr.agent.instrumentation.micronaut.http.client.ReactorListener;
import com.nr.agent.instrumentation.micronaut.http.client.ResponseConsumer;
import com.nr.agent.instrumentation.micronaut.http.client.Utils;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.sse.Event;
import io.reactivex.Flowable;

@Weave(originalName = "io.micronaut.http.client.netty.DefaultHttpClient", type = MatchType.ExactClass)
public abstract class DefaultHttpClient_Instrumentation {

    @Trace(dispatcher = true)
    public <I> Flowable<Event<ByteBuffer<?>>> eventStream(io.micronaut.http.HttpRequest<I> request) {
        return Weaver.callOriginal();
    }

    public <I, O, E> Flowable<io.micronaut.http.HttpResponse<O>> exchange(io.micronaut.http.HttpRequest<I> request, Argument<O> bodyType,
            Argument<E> errorType) {
        MicronautHeaders headers  = new MicronautHeaders(request);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);

        Flowable<io.micronaut.http.HttpResponse<O>> result = Weaver.callOriginal();
        HttpParameters params = HttpParameters.library("Micronaut")
                .uri(Utils.getRequestURI(request))
                .procedure(request.getMethodName())
                .noInboundHeaders()
                .build();
        Transaction txn = NewRelic.getAgent().getTransaction();
        ReactorListener listener = new ReactorListener(txn, params);
        return result.doOnSubscribe(listener).doOnCancel(listener).doOnTerminate(listener).doOnNext(new ResponseConsumer(txn));
    }

    public <I> Flowable<io.micronaut.http.HttpResponse<ByteBuffer<?>>> exchangeStream(io.micronaut.http.HttpRequest<I> request) {
        MicronautHeaders  headers  = new MicronautHeaders(request);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);

        Flowable<io.micronaut.http.HttpResponse<ByteBuffer<?>>> result = Weaver.callOriginal();
        HttpParameters params = HttpParameters.library("Micronaut")
                .uri(Utils.getRequestURI(request))
                .procedure(request.getMethodName())
                .noInboundHeaders()
                .build();
        Transaction txn = NewRelic.getAgent().getTransaction();
        ReactorListener listener = new ReactorListener(txn, params);
        return result.doOnSubscribe(listener).doOnCancel(listener).doOnTerminate(listener).doOnNext(new ResponseConsumer(txn));
    }

    @Trace(dispatcher = true)
    public <I, O> Flowable<O> jsonStream(io.micronaut.http.HttpRequest<I> request, io.micronaut.core.type.Argument<O> type) {
        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public Flowable<MutableHttpResponse<?>> proxy(io.micronaut.http.HttpRequest<?> request) {
        MicronautHeaders headers = new MicronautHeaders(request);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);

        Flowable<MutableHttpResponse<?>> result = Weaver.callOriginal();
        HttpParameters params = HttpParameters.library("Micronaut")
                .uri(Utils.getRequestURI(request))
                .procedure(request.getMethodName())
                .noInboundHeaders()
                .build();
        Transaction txn = NewRelic.getAgent().getTransaction();
        ReactorListener listener = new ReactorListener(txn, params);
        return result.doOnSubscribe(listener).doOnCancel(listener).doOnTerminate(listener).doOnNext(new ResponseConsumer(txn));
    }

}
