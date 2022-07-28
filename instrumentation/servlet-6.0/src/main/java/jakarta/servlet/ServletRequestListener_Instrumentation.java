/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package jakarta.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.servlet.ServletRequestEvent;

import java.util.logging.Level;

/**
 * We instrument ServletRequestListener in order to work around buggy servlet frameworks (I'm looking at you, Tomcat)
 * that don't reliably raise requestDestroyed events to match their requestInitialized events.
 */
@Weave(type = MatchType.Interface, originalName = "jakarta.servlet.ServletRequestListener")
public abstract class ServletRequestListener_Instrumentation {

    public void requestInitialized(ServletRequestEvent sre) {
        try {
            Weaver.callOriginal();
        } catch (RuntimeException t) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, t, "An exception occurred in the application" +
                    " during requestInitialized processing. The current transaction will be ignored. This may reduce" +
                    " the reported throughput.");

            Transaction transaction = AgentBridge.getAgent().getTransaction(false);
            if (transaction != null) {
                // Ignore this transaction and finish it because a listener threw an exception on init
                transaction.ignore();
                transaction.requestDestroyed();
            }
            throw t;
        }
    }

    public void requestDestroyed(ServletRequestEvent sre) {
        try {
            Weaver.callOriginal();
        } catch (RuntimeException t) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, t, "An exception occurred during requestDestroyed");
            Transaction transaction = AgentBridge.getAgent().getTransaction(false);
            if (transaction != null) {
                // Just finish this transaction because a listener threw an exception on destroy
                transaction.requestDestroyed();
            }
            throw t;
        }
    }

}
