/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.util.concurrent;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import util.TokenDelegateExecutor;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture")
public class CompletableFuture_Instrumentation<T> {

    private static Executor useTokenDelegateExecutor(Executor e) {
        if (null == e || e instanceof TokenDelegateExecutor) {
            return e;
        } else {
            return new TokenDelegateExecutor(e);
        }
    }

    private <V> CompletableFuture<V> uniApplyStage(
            Executor e, Function<? super T, ? extends V> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private CompletableFuture<Void> uniAcceptStage(Executor e,
            Consumer<? super T> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private CompletableFuture<Void> uniRunStage(Executor e, Runnable f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private CompletableFuture<T> uniWhenCompleteStage(
            Executor e, BiConsumer<? super T, ? super Throwable> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private <V> CompletableFuture<V> uniHandleStage(
            Executor e, BiFunction<? super T, Throwable, ? extends V> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private <V> CompletableFuture<V> uniComposeStage(
            Executor e, Function<? super T, ? extends CompletionStage<V>> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private <U, V> CompletableFuture<V> biApplyStage(
            Executor e, CompletionStage<U> o,
            BiFunction<? super T, ? super U, ? extends V> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private <U> CompletableFuture<Void> biAcceptStage(
            Executor e, CompletionStage<U> o,
            BiConsumer<? super T, ? super U> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private CompletableFuture<Void> biRunStage(Executor e, CompletionStage<?> o,
            Runnable f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private <U extends T, V> CompletableFuture<V> orApplyStage(
            Executor e, CompletionStage<U> o,
            Function<? super T, ? extends V> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private <U extends T> CompletableFuture<Void> orAcceptStage(
            Executor e, CompletionStage<U> o, Consumer<? super T> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    private CompletableFuture<Void> orRunStage(Executor e, CompletionStage<?> o,
            Runnable f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    static <U> CompletableFuture<U> asyncSupplyStage(Executor e,
            Supplier<U> f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    static CompletableFuture<Void> asyncRunStage(Executor e, Runnable f) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }

    //available since JDK 9
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor e) {
        e = useTokenDelegateExecutor(e);
        return Weaver.callOriginal();
    }


}
