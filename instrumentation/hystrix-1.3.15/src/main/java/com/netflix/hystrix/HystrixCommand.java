/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.netflix.hystrix;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;

import java.util.concurrent.Future;

/**
 * This class is responsible for joining all of the async work through all execution paths of the command in Hystrix.
 *
 * <p>
 * Hystrix 1.3 uses the magnificent {@link rx.Observable} framework to execute commands. In order to obtain the
 * observable, the framework calls {@link #toObservable(Scheduler, boolean)}. Then it will either call {@link #run()} to
 * execute the command, or {@link #getFallback()} if the system cannot run the command (e.g. the Hystrix circuit breaker
 * is tripped).
 *
 * <p>
 * We register <i>two</i> async contexts in {@link #toObservable(Scheduler, boolean)} because the command may get timed
 * out and {@link #getFallback()} executed even if {@link #run()} was executed. We start the async work in both cases,
 * and use {@link com.newrelic.agent.bridge.Agent#ignoreIfUnstartedAsyncContext(Object)} to make sure the other
 * registered context is ignored in all cases.
 *
 * <p>
 * This logic could be greatly simplified by switching to the new async token API, as a single token can be used to link
 * async work from many places, and would likely provide a noticeable performance improvement. See Hystrix 1.4
 * instrumentation for details.
 * 
 * @param <R>
 */
@Weave(type = MatchType.BaseClass)
public abstract class HystrixCommand<R> {

    @NewField
    private final Object timeoutAsyncContext;

    protected HystrixCommand(HystrixCommandGroupKey group) {
        timeoutAsyncContext = new Object();
    }

    protected HystrixCommand(HystrixCommand.Setter setter) {
        timeoutAsyncContext = new Object();
    }

    public abstract boolean isResponseTimedOut();

    @Trace(dispatcher = true)
    public abstract R execute();

    @Trace(dispatcher = true)
    public abstract Future<R> queue();

    @Trace(dispatcher = true)
    private R executeCommand() {
        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    protected R run() {
        // the command is being executed on a new thread; join this work to the parent transaction
        AgentBridge.getAgent().startAsyncActivity(this);
        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    protected R getFallback() {
        // attempt to join using the command context, and fallback to the timeout context if it's been started already
        if (AgentBridge.getAgent().startAsyncActivity(this)) {
            // ignore the timeout context - we just joined work using the command context
            AgentBridge.getAgent().ignoreIfUnstartedAsyncContext(timeoutAsyncContext);
        } else {
            // start using the timeout context - the command context has already been started
            AgentBridge.getAgent().startAsyncActivity(timeoutAsyncContext);
        }

        if (isResponseTimedOut()) {
            AgentBridge.privateApi.addCustomAttribute("TimedOut", "true");
        }

        return Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    private ObservableCommand<R> toObservable(Scheduler observeOn, boolean performAsyncTimeout) {
        ObservableCommand<R> output = Weaver.callOriginal();

        if (output.getClass().getName().equals("com.netflix.hystrix.HystrixCommand$CachedObservableResponse")) {
            NewRelic.getAgent().getTransaction().getTracedMethod().setMetricName("Java", getClass().getName(),
                    "toObservable", "ResultFromCache");
            // this is a cached result and so we do not need to join async work
            return output;
        }

        // for the happy path where either run() or getFallback() are called, but not both
        AgentBridge.getAgent().getTransaction().registerAsyncActivity(this);

        // the unhappy path, where a command is timed out after run() has started
        AgentBridge.getAgent().getTransaction().registerAsyncActivity(timeoutAsyncContext);

        // ignore the timeout context if the command has finished and it hasn't been started
        return new ObservableCommand<>(output.finallyDo(new Action0() {
            @Override
            public void call() {
                AgentBridge.getAgent().ignoreIfUnstartedAsyncContext(timeoutAsyncContext);
            }
        }), output.getCommand());
    }

    @Trace(dispatcher = true)
    public abstract Observable<R> observe();

    /**
     * This is only weaved so we can access the underlying {@link HystrixCommand} at the end of
     * {@link #toObservable(Scheduler, boolean)} so we can schedule the action to clean up the timeout context using
     * {@link ObservableCommand#finallyDo(Action0)}.
     *
     * @param <R>
     */
    @Weave
    private static class ObservableCommand<R> extends Observable<R> {
        ObservableCommand(final Observable<R> originalObservable, final HystrixCommand<R> command) {
            // this super call is here only to get it to compile - the weaver should remove it
            super(new OnSubscribe<R>() {
                @Override
                public void call(Subscriber<? super R> subscriber) {

                }
            });
        }

        public HystrixCommand<R> getCommand() {
            return Weaver.callOriginal();
        }
    }

    @Weave
    public static class Setter {
    }
}
