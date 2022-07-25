/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package jakarta.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.servlet.ServletContextEvent;

@Weave(type = MatchType.Interface, originalName = "jakarta.servlet.ServletContextListener")
public class ServletContextListener_Instrumentation {

    @Trace(dispatcher = true)
    public void contextInitialized(ServletContextEvent sce) {
        AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.SERVLET_NAME, false,
                "Initializer/ServletContextListener", getClass().getName(), "contextInitialized");
        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public void contextDestroyed(ServletContextEvent sce) {
        AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.SERVLET_NAME, false,
                "Initializer/ServletContextListener", getClass().getName(), "contextDestroyed");
        Weaver.callOriginal();
    }
}
