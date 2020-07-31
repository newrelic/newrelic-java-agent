/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.connector;

import javax.servlet.AsyncContext;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.tomcat.util.net.SocketEvent;

/**
 * This class handles async processing.
 * 
 * #TomcatServletRequestListener is not called by Tomcat if async is started, so must finish the transaction by calling
 * {@link Transaction#requestDestroyed()}.
 */
@Weave(originalName = "org.apache.catalina.connector.CoyoteAdapter")
public abstract class CoyoteAdapter_Weaved {

    /**
     * This method is called on an async dispatch.
     */
    public boolean asyncDispatch(org.apache.coyote.Request req, org.apache.coyote.Response res, SocketEvent status)
            throws Exception {

        // Compare this code with the similar code in the Tomcat-8.5.0 module. In 8.5.2 the protect field asyncContext
        // field was removed, and the method getAsyncContextInternal() was added, forcing us to create a whole separate
        // weave module.

        Request request = (Request) req.getNote(1);
        AsyncContext asyncContext = request.getAsyncContextInternal();
        if (asyncContext != null) {
            AgentBridge.asyncApi.resumeAsync(asyncContext);
        }

        boolean result = Weaver.callOriginal();

        checkSuspend(req);

        return result;

    }

    /**
     * This method is called on the initial request.
     */
    public void service(org.apache.coyote.Request req, org.apache.coyote.Response res) {

        Weaver.callOriginal();

        checkSuspend(req);

    }

    private void checkSuspend(org.apache.coyote.Request req) {
        Request request = (Request) req.getNote(1);
        if (request.isAsyncStarted()) {
            AsyncContext asyncContext = request.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
                AgentBridge.getAgent().getTransaction().requestDestroyed();
            }
        }
    }

}
