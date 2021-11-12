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
import com.newrelic.agent.bridge.jfr.events.supportability.transaction.completablefuture.CompletableFutureAsyncRunCreateEvent;
import com.newrelic.agent.bridge.jfr.events.supportability.transaction.completablefuture.CompletableFutureAsyncRunRunEvent;
import com.newrelic.agent.bridge.jfr.events.supportability.transaction.completablefuture.CompletableFutureAsyncSupplyCreateEvent;
import com.newrelic.agent.bridge.jfr.events.supportability.transaction.completablefuture.CompletableFutureAsyncSupplyRunEvent;
import com.newrelic.agent.bridge.jfr.events.supportability.transaction.completablefuture.CompletableFutureFinishEvent;
import com.newrelic.agent.bridge.jfr.events.supportability.transaction.completablefuture.CompletableFutureUniCompletionCreateEvent;
import com.newrelic.agent.bridge.jfr.events.supportability.transaction.completablefuture.CompletableFutureTryFireEvent;
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

        // TODO create JFR event for helping to diagnose CF instrumentation??
        UniCompletion(Executor executor, CompletableFuture_Instrumentation<V> dep, CompletableFuture_Instrumentation<T> src) {
            CompletableFutureUniCompletionCreateEvent completableFutureUniCompletionCreateEvent = new CompletableFutureUniCompletionCreateEvent();
            completableFutureUniCompletionCreateEvent.begin();

            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                completableFutureUniCompletionCreateEvent.transactionObject = tx.toString();
                completableFutureUniCompletionCreateEvent.uniCompletionObject = this.toString();

                if (dep.completableToken == null) {
                    dep.completableToken = tx.getToken();
                    if (dep.completableToken != null) {
                        completableFutureUniCompletionCreateEvent.tokenObject = dep.completableToken.toString();
                    }
                }
            }
            completableFutureUniCompletionCreateEvent.commit();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        CompletableFuture_Instrumentation<?> tryFire(int mode) {
            CompletableFutureTryFireEvent completableFutureTryFireEvent = new CompletableFutureTryFireEvent();
            completableFutureTryFireEvent.begin();
            completableFutureTryFireEvent.completableFutureObject = this.toString();

            if (null != dep.completableToken) {
                completableFutureTryFireEvent.tokenObject = dep.completableToken.toString();

                if (dep.completableToken.link()) {
                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "Completion", "tryFire");
                    completableFutureTryFireEvent.tokenLinked = true;
                }
            }
            CompletableFuture_Instrumentation<?> future = Weaver.callOriginal();
            completableFutureTryFireEvent.commit();

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
        CompletableFutureFinishEvent completableFutureFinishEvent = new CompletableFutureFinishEvent();
        completableFutureFinishEvent.begin();
        completableFutureFinishEvent.completableFutureObject = this.toString();

        if (this.completableToken != null) {
            completableFutureFinishEvent.tokenObject = this.completableToken.toString();

            this.completableToken.expire();
            this.completableToken = null;

            completableFutureFinishEvent.tokenExpired = true;
        }
        completableFutureFinishEvent.commit();
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncRun")
    static final class AsyncRun {

        @NewField
        private Token asyncToken;

        AsyncRun(CompletableFuture_Instrumentation<Void> dep, Runnable fn) {
            CompletableFutureAsyncRunCreateEvent completableFutureAsyncRunCreateEvent = new CompletableFutureAsyncRunCreateEvent();
            completableFutureAsyncRunCreateEvent.begin();
            completableFutureAsyncRunCreateEvent.asyncRunObject = this.toString();

            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
                completableFutureAsyncRunCreateEvent.transactionObject = tx.toString();

                if (this.asyncToken != null) {
                    completableFutureAsyncRunCreateEvent.tokenObject = this.asyncToken.toString();
                }
            }
            completableFutureAsyncRunCreateEvent.commit();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void run() {
            CompletableFutureAsyncRunRunEvent completableFutureAsyncRunRunEvent = new CompletableFutureAsyncRunRunEvent();
            completableFutureAsyncRunRunEvent.begin();
            completableFutureAsyncRunRunEvent.asyncRunObject = this.toString();

            if (null != this.asyncToken) {
                completableFutureAsyncRunRunEvent.tokenObject = this.asyncToken.toString();

                if (this.asyncToken.linkAndExpire()) {
                    completableFutureAsyncRunRunEvent.tokenExpired = true;

                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncRun", "run");
                }
                this.asyncToken = null;
            }
            completableFutureAsyncRunRunEvent.commit();

            Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "java.util.concurrent.CompletableFuture$AsyncSupply")
    static final class AsyncSupply<T> {

        @NewField
        private Token asyncToken;

        AsyncSupply(CompletableFuture_Instrumentation<T> dep, Supplier<T> fn) {
            CompletableFutureAsyncSupplyCreateEvent completableFutureAsyncSupplyCreateEvent = new CompletableFutureAsyncSupplyCreateEvent();
            completableFutureAsyncSupplyCreateEvent.begin();
            completableFutureAsyncSupplyCreateEvent.asyncSupplyObject = this.toString();

            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isStarted() && AgentBridge.getAgent().getTracedMethod().trackChildThreads()) {
                this.asyncToken = tx.getToken();
                completableFutureAsyncSupplyCreateEvent.transactionObject = tx.toString();

                if (this.asyncToken != null) {
                    completableFutureAsyncSupplyCreateEvent.tokenObject = this.asyncToken.toString();
                }
            }
            completableFutureAsyncSupplyCreateEvent.commit();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void run() {
            CompletableFutureAsyncSupplyRunEvent completableFutureAsyncSupplyRunEvent = new CompletableFutureAsyncSupplyRunEvent();
            completableFutureAsyncSupplyRunEvent.begin();
            completableFutureAsyncSupplyRunEvent.asyncSupplyObject = this.toString();

            if (null != this.asyncToken) {
                completableFutureAsyncSupplyRunEvent.tokenObject = this.asyncToken.toString();

                if (this.asyncToken.linkAndExpire()) {
                    completableFutureAsyncSupplyRunEvent.tokenExpired = true;

                    TracedMethod tm = (TracedMethod) AgentBridge.getAgent().getTransaction().getTracedMethod();
                    tm.setMetricName("Java", "CompletableFuture", "AsyncSupply", "run");
                }
                this.asyncToken = null;
            }
            completableFutureAsyncSupplyRunEvent.commit();

            Weaver.callOriginal();
        }
    }

}
