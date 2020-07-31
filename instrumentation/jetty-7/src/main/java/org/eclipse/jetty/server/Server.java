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
import com.newrelic.agent.instrumentation.jetty7.ServerHelper;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class Server {

    public abstract Connector[] getConnectors();

    public abstract ThreadPool getThreadPool();

    public void handle(HttpConnection connection) {
        Request request = connection.getRequest();
        boolean isStarted = AgentBridge.getAgent().getTransaction().isStarted();

        if (request != null && !isStarted) {
            ServerHelper.preHandle(request);
        }

        try {
            Weaver.callOriginal();
        } finally {
            if (request != null && !isStarted) {
                ServerHelper.postHandle(request);
            }
        }
    }

    public void handleAsync(HttpConnection connection) {
        Request request = connection.getRequest();
        boolean isStarted = AgentBridge.getAgent().getTransaction().isStarted();

        if (request != null && !isStarted) {
            ServerHelper.preHandle(request);
        }

        try {
            Weaver.callOriginal();
        } finally {
            if (request != null && !isStarted) {
                ServerHelper.postHandle(request);
            }
        }
    }

    protected void doStart() {
        AgentBridge.instrumentation.registerCloseable(Weaver.getImplementationTitle(),
                AgentBridge.privateApi.addSampler(new JettySampler(this), 1, TimeUnit.MINUTES));
        Weaver.callOriginal();
    }

    public static String getVersion() {
        return Weaver.callOriginal();
    }
}
