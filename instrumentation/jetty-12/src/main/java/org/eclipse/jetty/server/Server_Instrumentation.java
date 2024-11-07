/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty12.JettyRequest;
import com.nr.agent.instrumentation.jetty12.JettyResponse;
import com.nr.agent.instrumentation.jetty12.JettySampler;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.server.Server")
public abstract class Server_Instrumentation {

    protected void doStart() {
        AgentBridge.privateApi.addSampler(new JettySampler(this), 1, TimeUnit.MINUTES);
        Weaver.callOriginal();
    }

    public boolean handle(Request request, Response response, Callback callback) {
        boolean isStarted = AgentBridge.getAgent().getTransaction().isStarted();
        // if there is a #ContextHolder, then this is not embedded Jetty, so the transaction should start there
        boolean startTransaction = request != null && !isStarted;

        if (startTransaction) {
//            AgentBridge.getAgent().getTransaction(true)
//                    .requestInitialized(new JettyRequest(request), new JettyResponse(response));
            AgentBridge.getAgent().getLogger().log(Level.FINEST,
                    "NR-335227 commented out attempt to get or start transaction and call to requestInitialized()");

            // Log headers
            for(HttpField field : request.getHeaders()) {
                if (field.getValue() == null) {
                    AgentBridge.getAgent().getLogger().log(Level.FINEST,
                            "NR-335227 server.handle() request contains null header with name " + field.getName());
                } else if (field.getValue().isBlank()) {
                    AgentBridge.getAgent().getLogger().log(Level.FINEST, "NR-335227 server.handle() request contains blank header with name "
                            + field.getName()
                            + " and with length of "
                            + field.getValue().length());
                } else {
                    AgentBridge.getAgent().getLogger().log(Level.FINEST,
                            "NR-335227 server.handle() request contains filled header with name " + field.getName());

                }

            }
        }

        boolean result = false;
        try {
            result = Weaver.callOriginal();
        } finally {
            if (startTransaction) {
                AgentBridge.getAgent().getLogger().log(Level.FINEST, "NR-335227 request destroyed commented out");
//                AgentBridge.getAgent().getTransaction().requestDestroyed();
            }
        }

        return result;
    }

    public static String getVersion() {
        return Weaver.callOriginal();
    }

    public abstract Connector[] getConnectors();

    public abstract ThreadPool getThreadPool();
}
