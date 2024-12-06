/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http;

import org.apache.pekko.http.scaladsl.model.HttpRequest;
import org.apache.pekko.http.scaladsl.model.Uri;
import org.apache.pekko.http.scaladsl.server.Directive;
import org.apache.pekko.http.scaladsl.server.NewRelicRequestContextWrapper;
import org.apache.pekko.http.scaladsl.server.PathMatcher;
import org.apache.pekko.http.scaladsl.server.RequestContext;
import org.apache.pekko.http.scaladsl.server.RequestContextImpl;
import org.apache.pekko.http.scaladsl.server.RouteResult;
import org.apache.pekko.http.scaladsl.server.util.Tuple;
import com.newrelic.agent.bridge.AgentBridge;
import scala.Function1;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

public class PathMatcherUtils {

    /**
     * The purpose of this initializer is to hook into a place that's only called once during initialization of the
     * pekko-http library so we can work around an issue where our agent fails to transform the RequestContext class.
     */
    static {
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Retransforming org.apache.pekko.http.scaladsl.server.RequestContextImpl");
        AgentBridge.instrumentation.retransformUninstrumentedClass(RequestContextImpl.class);
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Retransformed org.apache.pekko.http.scaladsl.server.RequestContextImpl");
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Retransforming org.apache.pekko.http.scaladsl.server.NewRelicRequestContextWrapper");
        AgentBridge.instrumentation.retransformUninstrumentedClass(NewRelicRequestContextWrapper.class);
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Retransformed org.apache.pekko.http.scaladsl.server.NewRelicRequestContextWrapper");
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
     * @param path            the current path match to append to
     * @param matching        the type of match (Matched or Unmatched)
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
     * @param path     the current path match to append to
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
    public static void startRepeat(Uri.Path path) {
        NewRelicRequestContextWrapper ctx = nrRequestContext.get();
        if (ctx != null && !ctx.divertRepeat().get()) {
            ctx.divertRepeat().set(true);

            Deque<String> pathQueue = ctx.matchedPath();
            pathQueue.offer("(");
        }
    }

    /**
     * Mark the end of the repeating pattern by copying the temporary "repeatHolder" pattern into the main
     * matched path. A repeat pattern will look something like this:
     * <p>
     * "(IntNumber/).repeat()"
     *
     * @param path     the current path match to append to
     * @param matching the type of match (Matched or Unmatched)
     */
    public static void endRepeat(Uri.Path path, PathMatcher.Matching<?> matching) {
        final int MAX_APPENDED_SEGMENTS_TO_INCLUDE = 2;

        NewRelicRequestContextWrapper ctx = nrRequestContext.get();
        if (ctx == null) {
            return;
        }
        ctx.divertRepeat().set(false);

        if (matching instanceof PathMatcher.Matched) {
            PathMatcher.Matched<Tuple<?>> matched = (PathMatcher.Matched) matching;
            Deque<String> repeatPathQueue = ctx.repeatHolder();
            Deque<String> pathQueue = ctx.matchedPath();
            for (int i = 0; i < MAX_APPENDED_SEGMENTS_TO_INCLUDE; i++) {
                String queueResult = repeatPathQueue.pollFirst();
                if (queueResult != null) {
                    pathQueue.add(queueResult);
                }
            }
            pathQueue.offer(").repeat()");
        }

        ctx.repeatHolder(new LinkedBlockingDeque<String>());
    }

    /**
     * Add any static string matches to the path. This will be something like "/foo" or "/bar"
     *
     * @param path     the current path match to append to
     * @param prefix   the matched static prefix (hardcoded string)
     * @param matching the type of match (Matched or Unmatched)
     */
    public static void appendStaticString(Uri.Path path, String prefix, PathMatcher.Matching<?> matching) {
        if (matching.getClass().isAssignableFrom(matchedClass)) {
            insertPathValue(prefix);
        } else if (matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(path, prefix);
        }
    }

    /**
     * Add any regex matches to the path
     *
     * @param path         the current path match to append to
     * @param regexPattern the matched regex mattern
     * @param matching     the type of match (Matched or Unmatched)
     */
    public static void appendRegex(Uri.Path path, String regexPattern, PathMatcher.Matching<?> matching) {
        if (matching.getClass().isAssignableFrom(matchedClass)) {
            insertPathValue(regexPattern);
        } else if (matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(path, regexPattern);
        }
    }

    /**
     * Adds a negation ("!") to the path
     */
    public static void appendNegation() {
        insertPathValue("!");
    }

    /**
     * Adds a tilde ("~") to the path which represents a concatenation. There are a couple of special cases here for ignoring
     * the tilde if it's the first part of the path, if the previous item added was a slash or if we are in the middle of a repeat.
     * This helps clear up the final path value and get it as close to what the user entered as possible.
     *
     * @param path the current path match to append to
     */
    public static void appendTilde(Uri.Path path) {
        Deque<String> pathQueue = getPathQueue();

        NewRelicRequestContextWrapper ctx = nrRequestContext.get();
        if (pathQueue.isEmpty() && ctx != null && ctx.divertRepeat().get()) {
            // We are in the middle of a repeating segment, we don't want these to show up as tildes (~)
            return;
        }

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
     */
    public static void appendOptional() {
        insertPathValue("?");
    }

    /**
     * Adds a Slash ("/") to the path
     *
     * @param path the current path match to append to
     */
    public static void appendSlash(Uri.Path path, PathMatcher.Matching<?> matching) {
        if (matching.getClass().isAssignableFrom(matchedClass)) {
            Deque<String> pathQueue = getPathQueue();

            // Special case to help clean up extra tildes
            if (!pathQueue.isEmpty() && pathQueue.peekLast().equals("~")) {
                pathQueue.removeLast();
            }
            insertPathValue("/");
        } else if (!path.isEmpty() && matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(path, "/");
        }
    }

    /**
     * Adds a "Remaining" or "RemainingPath" to the end of the path to correspond to that matcher.
     *
     * @param type    the type of "Remaining" match
     * @param path    the current path match to append to
     * @param matched the type of match (Matched)
     */
    public static void appendRemaining(String type, Uri.Path path, PathMatcher.Matched<?> matched) {
        insertPathValue(type);
    }

    /**
     * This is used when a specific path partially matches and we have more to check via a tilde concatenation. If the
     * secondary match fails we need to handle the unmatch and clear out the path.
     *
     * @param matching the type of match.
     */
    public static void andThen(PathMatcher.Matching<?> matching, Uri.Path pathRest) {
        if (matching != null && matching.getClass().isAssignableFrom(unmatchedClass)) {
            handleUnmatched(pathRest, null);
        } else {
            NewRelicRequestContextWrapper ctx = nrRequestContext.get();
            if (ctx != null) {
                // We had a successful Match, record the current queue length on the context
                ctx.currentMatchedQueueLength().set(getPathQueue().size());
            }
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
     * @param path   the current path that failed to match
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
                } else if (pathQueue.peekLast().equals("~") && !path.isEmpty()) {
                    // If we got here, it means that we matched something and it is a concatenation so we may have paths
                    // to remove in order to get back to a last known "matching" state.
                    int expectedQueueSize = ctx.currentMatchedQueueLength().get() + 1; // Include the tilde (~)

                    // If the expected queue size is the same as the current path, they it means we have paths to remove
                    // so we need to set the current matched queue length back to the previous known match. Otherwise,
                    // we can move forward by setting the previous size equal to the new current size
                    if (expectedQueueSize == getPathQueue().size()) {
                        ctx.currentMatchedQueueLength().set(ctx.previousMatchedQueueLength().get());
                    } else {
                        ctx.previousMatchedQueueLength().set(ctx.currentMatchedQueueLength().get());
                    }

                    for (int i = getPathQueue().size(); i > expectedQueueSize - 1; i--) {
                        pathQueue.removeLast();
                    }

                } else if (prefix == null) {
                    for (int i = getPathQueue().size(); i > ctx.previousMatchedQueueLength().get(); i--) {
                        pathQueue.removeLast();
                    }
                } else if (ctx.divertRepeat().get()) {
                    // If we are in the middle of a repeat match, an "unmatched" is not an issue
                    return;
                } else {
                    int matchedQueueSize = ctx.currentMatchedQueueLength().get();
                    int previousQueueSize = ctx.previousMatchedQueueLength().get();
                    int expectedQueueSize = matchedQueueSize + 1; // Includes the trailing tilde or slash
                    if (matchedQueueSize > 0 && previousQueueSize == 0 && pathQueue.size() == expectedQueueSize) {
                        // This case is here to handle where we've matched a pathPrefix, a sub path has failed but we have more paths we want to check.
                        // If we just cleared out the queue here we would either get an UnknownRoute or an incorrect route. Setting the
                        // previousMatchedQueueLength here ensures that we keep the pathPrefix for additional checks
                        ctx.previousMatchedQueueLength().set(ctx.currentMatchedQueueLength().get());
                    } else {
                        pathQueue.clear();
                        ctx.regexHolder().clear();
                    }
                }
            } else {
                pathQueue.clear();
                ctx.regexHolder().clear();
            }
        }
    }

    public static void reset() {
        nrRequestContext.remove();
    }

    public static void setHttpRequest(HttpRequest request) {
        AgentBridge.getAgent().getTransaction().setWebRequest(new RequestWrapper(request));
    }

    public static class DirectiveWrapper<T> extends Directive<T> {

        private final Directive<T> underlying;

        public DirectiveWrapper(Tuple<T> ev, Directive<T> underlying) {
            super(ev);
            this.underlying = underlying;

            // Remove the current request context since we may be switching threads in this directive
            nrRequestContext.remove();
        }

        @Override
        public Function1<RequestContext, Future<RouteResult>> tapply(Function1<T, Function1<RequestContext, Future<RouteResult>>> f) {
            Function1<RequestContext, Future<RouteResult>> result = underlying.tapply(f);
            return result.compose(new AbstractFunction1<RequestContext, RequestContext>() {
                @Override
                public RequestContext apply(RequestContext requestContext) {
                    if (requestContext instanceof NewRelicRequestContextWrapper) {
                        // If we have a New Relic wrapped RequestContext we should set this back into the thread local so we can use it after this directive
                        nrRequestContext.set(((NewRelicRequestContextWrapper) requestContext));
                    }
                    return requestContext;
                }
            });
        }
    }
}
