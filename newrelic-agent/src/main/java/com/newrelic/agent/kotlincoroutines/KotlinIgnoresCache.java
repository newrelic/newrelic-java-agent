package com.newrelic.agent.kotlincoroutines;

import com.newrelic.agent.service.ServiceFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Class that stores ignored Coroutine components that are added programmatically rather than
 * via the configuration.  Used primarily by instrumentation modules built on top of Kotlin
 * Coroutines (e.g. Ktor)
 * This allows the instrumentation developer to add ignores without having the user enter them
 * in newrelic.yml
 */
class KotlinIgnoresCache {

    private static final Set<String> ignoredSuspends = new HashSet<String>();
    private static final Set<String> ignoredRegexSuspends = new HashSet<>();
    private static final Set<String> ignoredContinuations = new HashSet<>();
    private static final Set<String> ignoredRegexContinuations = new HashSet<>();
    private static final Set<String> ignoredScopes = new HashSet<>();
    private static final Set<String> ignoredRegexScopes = new HashSet<>();
    private static final Set<String> ignoredDispatched = new HashSet<>();
    private static final Set<String> ignoredRegExDispatched = new HashSet<>();

    protected static Set<String> getIgnoredSuspends() {
        return ignoredSuspends;
    }

    protected static void addIgnoredSuspend(String suspend) {
        ignoredSuspends.add(suspend);
    }

    protected static Set<String> getIgnoredRegexSuspends() {
        return ignoredRegexSuspends;
    }

    protected static void addIgnoredRegexSuspend(String suspendRegex) {
        ignoredRegexSuspends.add(suspendRegex);
    }

    protected static Set<String> getIgnoredContinuations() {
        return ignoredContinuations;
    }

    protected static void addIgnoredContinuation(String continuation) {
        ignoredContinuations.add(continuation);
    }

    protected static Set<String> getIgnoredRegexContinuations() {
        return ignoredRegexContinuations;
    }

    protected static void addIgnoredRegexContinuation(String continuationRegex) {
        ignoredRegexContinuations.add(continuationRegex);
    }

    protected static Set<String> getIgnoredScopes() {
        return ignoredScopes;
    }

    protected static void addIgnoredScope(String scope) {
        ignoredScopes.add(scope);
    }

    protected static Set<String> getIgnoredRegexScopes() {
        return ignoredRegexScopes;
    }

    protected static void addIgnoredRegexScope(String scopeRegex) {
        ignoredRegexScopes.add(scopeRegex);
    }

    protected static Set<String> getIgnoredDispatched() {
        return ignoredDispatched;
    }

    protected static void addIgnoredDispatched(String dispatched) {
        ignoredDispatched.add(dispatched);
    }

    protected static Set<String> getIgnoredRegexDispatched() {
        return ignoredRegExDispatched;
    }

    protected static void addIgnoredRegexDispatched(String dispatchedRegex) {
        ignoredRegExDispatched.add(dispatchedRegex);
    }
}
