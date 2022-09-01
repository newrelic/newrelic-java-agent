/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package jakarta.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.Enumeration;
import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "jakarta.servlet.ServletRequest")
public abstract class ServletRequest_Instrumentation {

    public void setAttribute(String name, Object o) {

        if ("com.newrelic.agent.IGNORE".equals(name)) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "Setting Ignore transaction to \"{0}\" using request attribute", o);
            if (o instanceof Boolean && (Boolean) o) {
                NewRelic.getAgent().getTransaction().ignore();
            }
        } else if ("com.newrelic.agent.IGNORE_APDEX".equals(name)) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "Setting Ignore Apdex to \"{0}\" using request attribute", o);
            if (o instanceof Boolean && (Boolean) o) {
                NewRelic.getAgent().getTransaction().ignoreApdex();
            }
        } else if ("com.newrelic.agent.APPLICATION_NAME".equals(name)) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "Set application name to \"{0}\" using request attribute", o);
            AgentBridge.getAgent().getTransaction().setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE, ((String) o));
        } else if ("com.newrelic.agent.TRANSACTION_NAME".equals(name)) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "Set transaction name to \"{0}\" using request attribute", o);
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_LOW, false,
                    "RequestAttribute", ((String) o));
        }

        Weaver.callOriginal();
    }

    public abstract Enumeration getParameterNames();

    public abstract String[] getParameterValues(String name);

    public abstract Object getAttribute(String name);
}
