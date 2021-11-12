package com.newrelic.agent.bridge.jfr.events;

/**
 * Constants for JFR custom events.
 */
public class JfrCustomEventConstants {
    // Namespace that JFR custom events get recorded under
    public static final String NR_NAMESPACE_PREFIX = "com.newrelic.agent.java.";

    // Categories for JFR custom events
    public static final String NEW_RELIC_JFR_EVENT = "New Relic JFR Event";
    public static final String EXTERNAL = "External";
    public static final String SUPPORTABILITY = "Supportability";
    public static final String INSTRUMENTATION = "Instrumentation";
    public static final String TRANSACTION = "Transaction";
    public static final String COMPLETABLE_FUTURE = "CompletableFuture";
    public static final String TOKEN = "Token";
}
