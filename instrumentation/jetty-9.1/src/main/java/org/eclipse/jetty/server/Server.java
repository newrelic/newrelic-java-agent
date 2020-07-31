/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.thread.ThreadPool;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty91.ServerHelper;

@Weave
public abstract class Server {

    public abstract Connector[] getConnectors();

    public abstract ThreadPool getThreadPool();

    protected void doStart() {
        AgentBridge.instrumentation.registerCloseable(Weaver.getImplementationTitle(),
                AgentBridge.privateApi.addSampler(new JettySampler(this), 1, TimeUnit.MINUTES));
        Weaver.callOriginal();
    }

    public void handle(HttpChannel<?> connection) {
        Request request = connection.getRequest();
        boolean isStarted = AgentBridge.getAgent().getTransaction().isStarted();
        // if there is a #ContextHolder, then this is not embedded Jetty, so the transaction should start there
        boolean hasContextHolder = ServerHelper.hasContextHandler();
        boolean startTransaction = request != null && !isStarted && !hasContextHolder;

        if (startTransaction) {
            ServerHelper.preHandle(request, request.getResponse());
        }
        try {
            Weaver.callOriginal();
        } finally {
            if (startTransaction) {
                ServerHelper.postHandle(request);
            }
        }
    }

    public static String getVersion() {
        return Weaver.callOriginal();
    }
}
