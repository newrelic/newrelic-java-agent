/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.spray;

import com.newrelic.agent.bridge.AgentBridge;
import shapeless.HList;
import spray.http.Uri;
import spray.routing.NewRelicRequestContextWrapper;
import spray.routing.PathMatcher;
import spray.routing.RequestContext;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

public class PathMatcherUtils {

    /**
     * The purpose of this initializer is to hook into a place that's only called once during initialization of the
     * spray-http library so we can work around an issue where our agent fails to transform the RequestContext class.
     */
    static {
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Retransforming spray.routing.RequestContext");
        AgentBridge.instrumentation.retransformUninstrumentedClass(RequestContext.class);
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Retransformed spray.routing.RequestContext");
    }

    public static final Class<?> matchedClass = PathMatcher.Matched.class;
    public static final Class<?> unmatchedClass = PathMatcher.Unmatched$.class;

    // This allows us to get access to the request context (where the assembled path information is stored)
    public static final ThreadLocal<NewRelicRequestContextWrapper> nrRequestContext =
            new ThreadLocal<NewRelicRequestContextWrapper>() {
                @Override
                protected NewRelicRequestContextWrapper initialValue() {
                    return null;
                }
            };

    /**
     * When we have a match on a portion of the path, this method appends one of the numeric types to the path. This
     * helps to prevent a metric explosion by replacing potentially dynamic values with static strings.
     * 
     * @param numberMatchType the numeric path type ("IntNumber", "HexIntNumber", etc)
     * @param path the current path match to append to
     * @param matching the type of match (Matched or Unmatched)
     */
    public static void appendNumberMatch(String numberMatchType, Uri.Path path, PathMatcher.Matching matching) {
        if (matching.getClass().isAssignableFrom(matchedClass)) {
            insertPathValue(numberMatchType);
        } else if (matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(path, numberMatchType);
        }
    }

    /**
     * Add any "Segment" matches to the path.
     * 
     * @param path the current path match to append to
     * @param matching the type of match (Matched or Unmatched)
     */
    public static void appendSegment(Uri.Path path, PathMatcher.Matching matching) {
        if (matching.getClass().isAssignableFrom(matchedClass)) {
            insertPathValue("Segment");
        } else if (matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(path, "Segment");
        }
    }

    /**
     * Mark the start of a repeating pattern
     */
    public static void startRepeat() {
        NewRelicRequestContextWrapper ctx = nrRequestContext.get();
        if (ctx != null && !ctx.divertRepeat().get()) {
            ctx.divertRepeat().set(true);

            Deque<String> pathQueue = ctx.matchedPath();
            pathQueue.offer("(");
        }
    }

    /**
     * Mark a repeating pattern. This will capture the end of the first pattern that repeats and store
     * it in a size-limited deque to ensure that we only report one of the repetitions and not all of them.
     *
     * @param path the current path match to append to
     */
    public static void recordRepeat(Uri.Path path) {
        NewRelicRequestContextWrapper ctx = nrRequestContext.get();
        if (ctx != null && !ctx.repeatHolder().isEmpty()) {
            Deque<String> repeatDeque = new LinkedBlockingDeque<>(ctx.repeatHolder().size());
            repeatDeque.addAll(ctx.repeatHolder());
            ctx.repeatHolder(repeatDeque);
        }
    }

    /**
     * Mark the end of the repeating pattern by copying the temporary "repeatHolder" pattern into the main
     * matched path. A repeat pattern will look something like this:
     *
     * "(IntNumber/IntNumber).repeat()"
     */
    public static void endRepeat(PathMatcher.Matching<HList> matching) {
        NewRelicRequestContextWrapper ctx = nrRequestContext.get();
        if (ctx != null && ctx.divertRepeat().get()) {
            ctx.divertRepeat().set(false);

            Deque<String> repeatPathQueue = ctx.repeatHolder();
            Deque<String> pathQueue = ctx.matchedPath();
            boolean repeatMatched = !repeatPathQueue.isEmpty();
            if (matching.getClass().isAssignableFrom(matchedClass)) {
                PathMatcher.Matched matched = PathMatcher.Matched.class.cast(matching);
                repeatMatched = repeatMatched || matched.pathRest().isEmpty();
            }

            if (repeatMatched) {
                pathQueue.addAll(repeatPathQueue);
                pathQueue.offer(").repeat()");
            } else {
                pathQueue.clear();
            }

            ctx.repeatHolder(new LinkedBlockingDeque<String>());
        }
    }

    /**
     * Add any static string matches to the path. This will be something like "/foo" or "/bar"
     *
     * @param path the current path match to append to
     * @param prefix the matched static prefix (hardcoded string)
     * @param matching the type of match (Matched or Unmatched)
     */
    public static void appendStaticString(Uri.Path path, String prefix, PathMatcher.Matching matching) {
        if (matching.getClass().isAssignableFrom(matchedClass)) {
            insertPathValue(prefix);
        } else if (matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(path, prefix);
        }
    }

    /**
     * Add any regex matches to the path
     *
     * @param path the current path match to append to
     * @param regexPattern the matched regex mattern
     * @param matching the type of match (Matched or Unmatched)
     */
    public static void appendRegex(Uri.Path path, String regexPattern, PathMatcher.Matching matching) {
        if (matching.getClass().isAssignableFrom(matchedClass)) {
            insertPathValue(regexPattern);
        } else if (matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(path, regexPattern);
        }
    }

    /**
     * Adds a negation ("!") to the path
     *
     * @param path the current path match to append to
     */
    public static void appendNegation(Uri.Path path) {
        insertPathValue("!");
    }

    /**
     * Adds a tilde ("~") to the path which represents a concatenation. There is a special case here for ignoring
     * the tilde if it's the first part of the path or if the previous item added was a slash. This helps clear up
     * the final value and get it as close to what the user entered as possible.
     *
     * @param path the current path match to append to
     */
    public static void appendTilde(Uri.Path path) {
        Deque<String> pathQueue = getPathQueue();
        if (pathQueue.isEmpty() || !pathQueue.peekLast().equals("/")) {
            insertPathValue("~");
        }
    }

    /**
     * Adds a pipe ("|") to the path
     *
     * @param path the current path match to append to
     */
    public static void appendPipe(Uri.Path path) {
        insertPathValue("|");
    }

    /**
     * Adds an optional ("?") to the path
     *
     * @param path the current path match to append to
     */
    public static void appendOptional(Uri.Path path) {
        insertPathValue("?");
    }

    /**
     * Adds a Slash ("/") to the path
     *
     * @param path the current path match to append to
     */
    public static void appendSlash(Uri.Path path, PathMatcher.Matching matching) {
        if (matching.getClass().isAssignableFrom(matchedClass)) {
            Deque<String> pathQueue = getPathQueue();

            // Special case to help clean up extra tildes
            if (!pathQueue.isEmpty() && pathQueue.peekLast().equals("~")) {
                pathQueue.removeLast();
            }
            insertPathValue("/");
        } else if (matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(path, "/");
        }
    }

    /**
     * Adds a "Rest" or "RestPath" to the end of the path to correspond to that matcher.
     *
     * @param type the type of "Rest" match
     * @param path the current path match to append to
     * @param matched the type of match (Matched)
     */
    public static void appendRest(String type, Uri.Path path, PathMatcher.Matched matched) {
        insertPathValue(type);
    }

    /**
     * This is used when a specific path partially matches and we have more to check via a tilde concatenation. If the
     * secondary match fails we need to handle the unmatch and clear out the path.
     * 
     * @param matching the type of match. Only Unmatched is handled here
     */
    public static void andThen(PathMatcher.Matching<?> matching) {
        if (matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(null, null);
        }
    }
    
    /**
     * Finishes the current path by gathering up all of the stored values in the Deque and building up a string
     * to use as a the transaction name.
     *
     * @param ctx the wrapped RequestContext holding the path information
     * @return the transaction name for this path
     */
    public static String finishPathAndGetTransactionName(NewRelicRequestContextWrapper ctx) {
        Deque<String> pathElements = ctx.matchedPath();

        StringBuilder finalPath = new StringBuilder();
        if (pathElements == null) {
            finalPath.append("Unknown Route");
        } else {
            // First, do some cleanup
            if (!pathElements.isEmpty() && pathElements.peekLast().equals("~")) {
                pathElements.removeLast();
            }
            if (!pathElements.isEmpty() && !pathElements.peekFirst().equals("/")) {
                pathElements.addFirst("/");
            }

            for (String pathElement; (pathElement = pathElements.poll()) != null; ) {
                // More cleanup
                if (pathElement.equals("|")) {
                    continue;
                }

                finalPath.append(pathElement);
            }

            pathElements.clear();
        }

        String finalPathString = finalPath.toString();
        return finalPathString.isEmpty() ? "Unknown Route" : finalPathString;
    }

    /**
     * Handles inserting the new path value into the Deque as well as special-case logic for optionals
     *
     * @param pathValue the value to insert
     */
    private static void insertPathValue(String pathValue) {
        Deque<String> pathQueue = getPathQueue();
        if (!pathQueue.isEmpty() && pathQueue.peekLast().equals("!")) {
            // The result during a "negation" matched, which means this shouldn't match
            pathQueue.clear();
            return;
        }

        boolean previousOptional = !pathQueue.isEmpty() && pathQueue.peekLast().equals("?");
        if (previousOptional) {
            pathQueue.removeLast();
        }
        pathQueue.offer(pathValue);
        if (previousOptional) {
            pathQueue.offer(".?");
        }
    }

    private static Deque<String> getPathQueue() {
        NewRelicRequestContextWrapper ctx = nrRequestContext.get();
        if (ctx != null) {
            if (ctx.divertRepeat().get()) {
                return ctx.repeatHolder();
            }
            return ctx.matchedPath();
        }
        return new LinkedBlockingDeque<>();
    }

    /**
     * Since we came across an "unmatched" that means we'll need to erase our previous progress as it didn't match
     * anything. The one special case here is if the previous operator was a negation ("!") and in that case we
     * want to continue attempting to match.
     *
     * @param path the current path that failed to match
     * @param prefix in the case of a negation this is the value following it (the value to be negated)
     */
    private static void handleUnmatched(Uri.Path path, String prefix) {
        NewRelicRequestContextWrapper ctx = nrRequestContext.get();
        if (ctx != null) {
            Deque<String> pathQueue = getPathQueue();
            if (!pathQueue.isEmpty()) {
                if (pathQueue.peekLast().equals("!")) {
                    pathQueue.offer(prefix); // !{prefix}
                } else if (pathQueue.peekLast().equals("|")) {
                    // Pipe ("|") here means that the first match failed, but the second might
                    // not so we want to remove the pipe marker and let the match continue
                    pathQueue.removeLast();
                } else if (pathQueue.peekLast().equals("?")) {
                    pathQueue.removeLast();
                    pathQueue.offer(prefix);
                    pathQueue.offer(".?");
                } else if (ctx.divertRepeat().get()) {
                    // If we are in the middle of a repeat match, an "unmatched" is not an issue
                    return;
                } else {
                    pathQueue.clear();
                }
            } else {
                pathQueue.clear();
            }
        }
    }

    public static void reset() {
        nrRequestContext.remove();
    }
}
