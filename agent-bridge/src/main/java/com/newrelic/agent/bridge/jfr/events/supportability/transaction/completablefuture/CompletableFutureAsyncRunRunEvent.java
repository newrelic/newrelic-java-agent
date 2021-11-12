package com.newrelic.agent.bridge.jfr.events.supportability.transaction.completablefuture;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.COMPLETABLE_FUTURE;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.NEW_RELIC_JFR_EVENT;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.NR_NAMESPACE_PREFIX;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.SUPPORTABILITY;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.TRANSACTION;

@Name(CompletableFutureAsyncRunRunEvent.NAME)
@Category({ NEW_RELIC_JFR_EVENT, SUPPORTABILITY, TRANSACTION, COMPLETABLE_FUTURE })
@Label("CompletableFuture$AsyncRun Run")
@Description("CompletableFuture instrumentation info")
@StackTrace(true)
public class CompletableFutureAsyncRunRunEvent extends Event {
    static final String NAME = NR_NAMESPACE_PREFIX + "CompletableFutureAsyncRunRun";

    @Label("Token Expired")
    public boolean tokenExpired;

    @Label("CompletableFuture$AsyncRun Object")
    public String asyncRunObject;

    @Label("Token Object")
    public String tokenObject;
}
