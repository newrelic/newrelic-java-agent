/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

import java.io.IOException;
import java.util.logging.Level;

import static com.nr.agent.instrumentation.tomcat_request_listener.TomcatRequestListenerHelper.requestDestroyedNeeded;

@Weave
final class StandardHostValve {

    /**
     * This is the entry and exit point for a request in Tomcat. If we see a requestInitialized but no corresponding
     * requestDestroyed then we need to manually finish this transaction ourselves or we will potentially leak an
     * unfinished Transaction and cause extremely long response times to be reported from "requestInitialized()"
     */
    public final void invoke(Request request, Response response) throws IOException, ServletException {
        requestDestroyedNeeded.set(false);
        boolean nrAsyncAtStart = request.isAsync();

        try {
            Weaver.callOriginal();
        } finally {
            try {
                // This logic (aside from the requestDestroyedNeeded check) comes directly from Tomcat
                if (requestDestroyedNeeded.get() && !request.isAsync() && (!nrAsyncAtStart || !response.isError())) {
                    Transaction transaction = AgentBridge.getAgent().getTransaction(false);
                    if (transaction != null) {
                        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Missing required requestDestroyed call. " +
                                "Manually destroying transaction: {0}", transaction);
                        transaction.requestDestroyed();
                    }
                }
            } finally {
                // Ensure that we always reset this when we leave this method
                requestDestroyedNeeded.set(false);
            }
        }
    }
}
