/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.util.concurrent;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.function.Supplier;

@Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture")
public class CompletableFuture_Instrumentation<T> {

    @NewField
    public Token completableToken;

    @Weave(type = MatchType.BaseClass, originalName = "java.util.concurrent.CompletableFuture$UniCompletion")
    abstract static class UniCompletion<T,V> {

        CompletableFuture_Instrumentation<V> dep = Weaver.callOriginal();

        UniCompletion(Executor executor, CompletableFuture_Instrumentation<V> dep, CompletableFuture_Instrumentation<T> src) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                if (dep.completableToken == null) {
                    dep.completableToken = tx.getToken();
                }
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        CompletableFuture_Instrumentation<?> tryFire(int mode) {
            if (null != dep.completableToken) {
                if (dep.completableToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "Completion", "tryFire");
                }
            }
            CompletableFuture_Instrumentation<?> future = Weaver.callOriginal();
            return future;
        }
    }

    /*
     * The following methods are all the possible internal completion methods
     * that allow us to know when this CompletableFuture is done so we can expire
     * any tokens that we've created and used.
     */

    final boolean internalComplete(Object r) {
        boolean result = Weaver.callOriginal();
        finishCompletableFuture();
        return result;
    }

    final boolean completeNull() {
        boolean result = Weaver.callOriginal();
        finishCompletableFuture();
        return result;
    }

    final boolean completeValue(T t) {
        boolean result = Weaver.callOriginal();
        finishCompletableFuture();
        return result;
    }

    final boolean completeThrowable(Throwable x) {
        boolean result = Weaver.callOriginal();
        finishCompletableFuture();
        return result;
    }

    final boolean completeThrowable(Throwable x, Object r) {
        boolean result = Weaver.callOriginal();
        finishCompletableFuture();
        return result;
    }

    final boolean completeRelay(Object r) {
        boolean result = Weaver.callOriginal();
        finishCompletableFuture();
        return result;
    }

    /**
     * Expire any tokens that we've created and used on this CompletableFuture since it is now finished executing
     */
    private void finishCompletableFuture() {
        if (this.completableToken != null) {
            this.completableToken.expire();
            this.completableToken = null;
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncRun")
    static final class AsyncRun {

        @NewField
        private Token asyncToken;

        AsyncRun(CompletableFuture_Instrumentation<Void> dep, Runnable fn) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void run() {
            if (null != this.asyncToken) {
                if (this.asyncToken.linkAndExpire()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncRun", "run");
                }
                this.asyncToken = null;
            }
            Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncSupply")
    static final class AsyncSupply<T> {

        @NewField
        private Token asyncToken;

        AsyncSupply(CompletableFuture_Instrumentation<T> dep, Supplier<T> fn) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void run() {
            if (null != this.asyncToken) {
                if (this.asyncToken.linkAndExpire()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncSupply", "run");
                }
                this.asyncToken = null;
            }
            Weaver.callOriginal();
        }
    }

}
