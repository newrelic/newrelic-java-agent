/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server.handler;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty10.ServerHelper;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.server.handler.ContextHandler")
public abstract class ContextHandler {

    public ContextHandler() {
        ServerHelper.contextHandlerFound();
    }

    /**
     * This should be the Transaction start point if Jetty is not embedded. It's a fix for JAVA-877: Agent can block
     * finishing a transaction in Jetty. When the transaction start point was in #Server handle, the Agent could block
     * in #HttpInput read when finishing the transaction. Starting the transaction in #ContextHandler fixed the problem.
     */
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        boolean isStarted = AgentBridge.getAgent().getTransaction().isStarted();

        if (request != null && !isStarted) {
            ServerHelper.preHandle(baseRequest, response);
        }

        try {
            Weaver.callOriginal();
        } finally {
            if (request != null && !isStarted) {
                ServerHelper.postHandle(baseRequest);
            }
        }
    }

    public abstract String getContextPath();
}
