/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weaver;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VertxUtil {

    public static final String NEWRELIC_TOKEN = "newrelic-token";
    public static final String UNNAMED_PATH = "UnnamedPath";

    private static final String FRAMEWORK = "Vertx";
    private static final String NEWRELIC_PATH = "newrelic-path";

    // Java < 21  = org.example.Class$$Lambda$14/0x0000000800000a08@3ac42916
    // Java >= 21 = org.example.Class$$Lambda/0x00000008000c2a00@6442b0a6
    private static final Pattern lambda = Pattern.compile("\\$\\$Lambda(\\$[^/]*)?/.*");
    private static final Pattern instance = Pattern.compile("@.*");

    private VertxUtil() {
    }

    private static void nameTransaction(RoutingContext context, Transaction transaction) {
        if (transaction == null) {
            return;
        }

        Queue<String> pathQueue = context.get(NEWRELIC_PATH);
        if (pathQueue == null) {
            return;
        }

        transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, FRAMEWORK, buildName(context, pathQueue));
        context.data().remove(NEWRELIC_PATH);
    }

    public static void nameSegment(Handler<RoutingContext> handler) {
        // Avoid regex matching if not in txn
        if (handler != null && AgentBridge.getAgent().getTransaction(false) != null) {
            String metricName = lambda.matcher(handler.toString()).replaceAll(".lambda()");
            metricName = instance.matcher(metricName).replaceAll("");
            NewRelic.getAgent().getTracedMethod().setMetricName(metricName);
        }
    }

    public static void pushPath(RoutingContext context, String path) {
        if (AgentBridge.getAgent().getTransaction(false) == null) {
            return;
        }

        Queue<String> pathQueue = context.get(NEWRELIC_PATH);
        if (pathQueue == null) {
            pathQueue = new LinkedList<String>();
            context.put(NEWRELIC_PATH, pathQueue);
        }

        /*
         This can happen when we have a handler that applies to multiple routes

         router.route().handler(BodyHandler.create());
         router.route("/some/path").handler(BodyHandler.create());

         When the first handler matches, we'll push UNNAMED_PATH. The second handler has a path.
         We pop to name the transaction /some/path and not /unnamedPath/some/path

         This also works when there's more than one handler without a path/regex.
        */
        if (!pathQueue.isEmpty() && pathQueue.peek().equals(UNNAMED_PATH)) {
            pathQueue.poll();
        }

        // In the case of a re-route, we might hit a BodyHandler a second time. We want to ignore this.
        if (!pathQueue.isEmpty() && path.equals(UNNAMED_PATH)) {
            return;
        }

        pathQueue.add(path);
    }

    private static String buildName(RoutingContext context, Queue<String> pathQueue) {
        return pathQueue.stream().collect(Collectors.joining("/")) + " (" + context.request().method() + ")";
    }

    public static void link(RoutingContext context) {
        Token token = context.get(NEWRELIC_TOKEN);
        if (token != null) {
            token.link();
        }
    }

    // Sets the transaction name from the current path queue without clearing it.
    // Called from iterateNext() so the name is committed while still inside the dispatcher
    // trace scope; async endpoints would otherwise get a stale name after the scope exits.
    public static void nameTransactionEarly(RoutingContext context) {
        if (AgentBridge.getAgent().getTransaction(false) == null) {
            return;
        }
        Queue<String> pathQueue = context.get(NEWRELIC_PATH);
        if (pathQueue == null || pathQueue.isEmpty()) {
            return;
        }
        NewRelic.getAgent().getTransaction()
                .setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, FRAMEWORK, buildName(context, pathQueue));
    }

    // Clears the accumulated path queue. Called from restart() on reroute so the re-dispatched
    // route builds a fresh path rather than appending to the original route's path.
    public static void clearPath(RoutingContext context) {
        context.data().remove(NEWRELIC_PATH);
    }

    public static Handler<Void> expireAndNameTxnHandler(RoutingContext context) {
        return event -> {
            try {
                com.newrelic.agent.bridge.Token token = context.get(NEWRELIC_TOKEN);
                if (token != null) {
                    context.data().remove(NEWRELIC_TOKEN);
                    // Use the current thread's transaction (SameOrHigherPriority policy) rather than
                    // token.getTransaction() (HigherPriority policy) so that FRAMEWORK_HIGH can
                    // override any SERVLET_NAME that Netty may have stamped after nameTransactionEarly.
                    nameTransaction(context, NewRelic.getAgent().getTransaction());
                    token.expire();
                }
            } catch (Throwable t) {
                AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
            }
        };
    }

    public static Handler<HttpServerRequest> wrapRequestHandler(final Handler<HttpServerRequest> originalRequestHandler) {
        return new Handler<HttpServerRequest>() {
            @Override
            @Trace(dispatcher = true)
            public void handle(HttpServerRequest event) {
                originalRequestHandler.handle(event);
            }
        };
    }
}
