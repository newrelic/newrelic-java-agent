/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty12.JettyRequest;
import com.nr.agent.instrumentation.jetty12.JettyResponse;
import com.nr.agent.instrumentation.jetty12.JettySampler;
import com.nr.agent.instrumentation.jetty12.ServerHelper;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.concurrent.TimeUnit;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.server.Server")
public abstract class Server_Instrumentation {

    protected void doStart() {
        AgentBridge.privateApi.addSampler(new JettySampler(this), 1, TimeUnit.MINUTES);
        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public boolean handle(Request request, Response response, Callback callback) {
        Transaction txn = AgentBridge.getAgent().getTransaction(true);
        txn.setWebRequest(new JettyRequest(request));
        txn.setWebResponse(new JettyResponse(response));
        return Weaver.callOriginal();
    }

    public static String getVersion() {
        return Weaver.callOriginal();
    }

    public abstract Connector[] getConnectors();

    public abstract ThreadPool getThreadPool();
}
