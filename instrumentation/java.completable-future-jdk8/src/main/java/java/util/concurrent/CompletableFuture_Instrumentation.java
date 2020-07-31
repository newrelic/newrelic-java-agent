/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.util.concurrent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture")
public class CompletableFuture_Instrumentation<T> {

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncRun")
    static final class AsyncRun {

        @NewField
        private Token asyncToken;

        AsyncRun(Runnable fn, CompletableFuture<Void> dst) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public final boolean exec() {
            if (null != this.asyncToken) {
                if (this.asyncToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncRun", "exec");
                }
                this.asyncToken.expire();
                this.asyncToken = null;
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncSupply")
    static final class AsyncSupply<U> {

        @NewField
        private Token asyncToken;

        AsyncSupply(Supplier<U> fn, CompletableFuture<U> dst) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public final boolean exec() {
            if (null != this.asyncToken) {
                if (this.asyncToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncSupply", "exec");
                }
                this.asyncToken.expire();
                this.asyncToken = null;
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncApply")
    static final class AsyncApply<T, U> {

        @NewField
        private Token asyncToken;

        AsyncApply(T arg, Function<? super T, ? extends U> fn, CompletableFuture<U> dst) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public final boolean exec() {
            if (null != this.asyncToken) {
                if (this.asyncToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncApply", "exec");
                }
                this.asyncToken.expire();
                this.asyncToken = null;
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncCombine")
    static final class AsyncCombine<T, U, V> {

        @NewField
        private Token asyncToken;

        AsyncCombine(T arg1, U arg2, BiFunction<? super T, ? super U, ? extends V> fn, CompletableFuture<V> dst) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public final boolean exec() {
            if (null != this.asyncToken) {
                if (this.asyncToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncCombine", "exec");
                }
                this.asyncToken.expire();
                this.asyncToken = null;
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncAccept")
    static final class AsyncAccept<T> {

        @NewField
        private Token asyncToken;

        AsyncAccept(T arg, Consumer<? super T> fn, CompletableFuture<?> dst) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public final boolean exec() {
            if (null != this.asyncToken) {
                if (this.asyncToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncAccept", "exec");
                }
                this.asyncToken.expire();
                this.asyncToken = null;
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncAcceptBoth")
    static final class AsyncAcceptBoth<T, U> {

        @NewField
        private Token asyncToken;

        AsyncAcceptBoth(T arg1, U arg2, BiConsumer<? super T, ? super U> fn, CompletableFuture<?> dst) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public final boolean exec() {
            if (null != this.asyncToken) {
                if (this.asyncToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncAcceptBoth", "exec");
                }
                this.asyncToken.expire();
                this.asyncToken = null;
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncCompose")
    static final class AsyncCompose<T, U> {

        @NewField
        private Token asyncToken;

        AsyncCompose(T arg, Function<? super T, ? extends CompletionStage<U>> fn, CompletableFuture<U> dst) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public final boolean exec() {
            if (null != this.asyncToken) {
                if (this.asyncToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncCompose", "exec");
                }
                this.asyncToken.expire();
                this.asyncToken = null;
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncWhenComplete")
    static final class AsyncWhenComplete<T> {

        @NewField
        private Token asyncToken;

        AsyncWhenComplete(T arg1, Throwable arg2, BiConsumer<? super T, ? super Throwable> fn,
                CompletableFuture<T> dst) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public final boolean exec() {
            if (null != this.asyncToken) {
                if (this.asyncToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncWhenComplete", "exec");
                }
                this.asyncToken.expire();
                this.asyncToken = null;
            }
            return Weaver.callOriginal();
        }
    }

}
