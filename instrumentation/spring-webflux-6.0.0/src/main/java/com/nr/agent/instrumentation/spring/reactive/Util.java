/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spring.reactive;

import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.function.Function;

public class Util {

    public static final String NR_TXN_NAME = "newrelic-transaction-name";
    public static final String NR_TOKEN = "newrelic-token";

    public static <T> Mono<T> setTransactionToken(Mono<T> mono, Token token) {
        return mono.<T>transform(tokenLift(token));
    }

    public static <T> Function<? super Mono<T>, ? extends Publisher<T>> tokenLift(
            Token token) {
        return Operators.lift(
                (scannable, subscriber) -> new TokenLinkingSubscriber<T>(subscriber, token));
    }

    public static class TokenLinkingSubscriber<T> implements CoreSubscriber<T> {

        private final CoreSubscriber<? super T> subscriber;
        private final Context context;

        public TokenLinkingSubscriber(
                CoreSubscriber<? super T> subscriber, Token token) {
            this.subscriber = subscriber;
            this.context = subscriber.currentContext().put(NR_TOKEN, token);
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscriber.onSubscribe(s);
        }

        @Override
        public void onNext(T t) {
            subscriber.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            withNRError(() -> subscriber.onError(t), t);
        }

        @Override
        public void onComplete() {
            withNRToken(() -> subscriber.onComplete());
        }

        @Override
        public Context currentContext() {
            return context;
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        private void withNRToken(Runnable runnable) {
            Token token = currentContext().get(NR_TOKEN);
            if (token != null) {
                token.linkAndExpire();
            }
            runnable.run();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        private void withNRError(Runnable runnable, Throwable throwable) {
            Token token = currentContext().get(NR_TOKEN);
            if (token != null && token.isActive()) {
                token.linkAndExpire();
                NewRelic.noticeError(throwable);
            }
            runnable.run();
        }
    }

    public static RequestPredicate createRequestPredicate(final String name,
                                                          final RequestPredicate originalRequestPredicate) {
        return new RequestPredicate() {
            @Override
            public boolean test(ServerRequest request) {
                final boolean matched = originalRequestPredicate.test(request);
                if (matched) {
                    Util.addPath(request, "QueryParameter/" + name);
                }
                return matched;
            }

            @Override
            public String toString() {
                return "";
            }
        };
    }

    public static RequestPredicate createPathExtensionPredicate(String extension,
                                                                RequestPredicate originalRequestPredicate) {
        return new RequestPredicate() {
            @Override
            public boolean test(ServerRequest request) {
                final boolean matched = originalRequestPredicate.test(request);
                if (matched) {
                    Util.addPath(request, "PathExtension/" + extension);
                }
                return matched;
            }

            @Override
            public String toString() {
                return "";
            }
        };
    }

    public static void addPath(ServerRequest request, String name) {
        Token token = (Token) request.attributes().get(NR_TOKEN);
        if (token != null && !name.isEmpty()) {
            request.attributes().computeIfAbsent(NR_TXN_NAME, k -> "");
            String existingName = (String) request.attributes().get(NR_TXN_NAME);
            request.attributes().put(NR_TXN_NAME, existingName + name);
        }
    }
}
