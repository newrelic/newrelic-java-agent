package com.nr.instrumentation.reactor;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.nr.instrumentation.ReactorConfig;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implementation of a reactor.core.CoreSubscriber (a Context aware subscriber) that can be added as
 * a lifecycle hook on Flux/Mono operators to propagate, retrieve, and link Tokens across async contexts.
 *
 * Based on OpenTelemetry code:
 * https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/reactor-3.1/library/src/main/java/io/opentelemetry/instrumentation/reactor/TracingSubscriber.java
 * @param <T>
 */
public class TokenLinkingSubscriber<T> implements CoreSubscriber<T> {
    private final Token token;
    private final Subscriber<? super T> subscriber;
    private Context context;

    public TokenLinkingSubscriber(Subscriber<? super T> subscriber, Context ctx) {
        this.subscriber = subscriber;
        this.context = ctx;
        // newrelic-token is added by spring-webflux instrumentation of ServerWebExchange
        this.token = ctx.getOrDefault("newrelic-token", null);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        withNRToken(() -> subscriber.onSubscribe(subscription));
    }

    @Override
    public void onNext(T o) {
        withNRToken(() -> subscriber.onNext(o));
    }

    @Override
    public void onError(Throwable throwable) {
        withNRError(() -> subscriber.onError(throwable), throwable);
    }

    @Override
    public void onComplete() {
        withNRToken(subscriber::onComplete);
    }

    @Override
    public Context currentContext() {
        return context;
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    private void withNRToken(Runnable runnable) {
        if (token != null && AgentBridge.getAgent().getTransaction(false) == null) {
            token.link();
        }
        runnable.run();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    private void withNRError(Runnable runnable, Throwable throwable) {
        if (token != null && token.isActive()) {
            token.link();
            if (ReactorConfig.errorsEnabled) {
                NewRelic.noticeError(throwable);
            }
        }
        runnable.run();
    }

    public static <T> Function<? super Publisher<T>, ? extends Publisher<T>> tokenLift() {
        return Operators.lift(new TokenLifter<>());
    }

    private static class TokenLifter<T>
            implements BiFunction<Scannable, CoreSubscriber<? super T>, CoreSubscriber<? super T>> {

        public TokenLifter() {
        }

        @Override
        public CoreSubscriber<? super T> apply(Scannable publisher, CoreSubscriber<? super T> sub) {
            // if Flux/Mono #just, #empty, #error
            if (publisher instanceof Fuseable.ScalarCallable) {
                return sub;
            }
            Token token = sub.currentContext().getOrDefault("newrelic-token", null);
            if (token != null ) {
                return new TokenLinkingSubscriber<>(sub, sub.currentContext());
            }
            return sub;
        }
    }
}