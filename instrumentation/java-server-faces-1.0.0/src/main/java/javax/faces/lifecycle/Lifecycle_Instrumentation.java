/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package javax.faces.lifecycle;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;

@Weave(type = MatchType.BaseClass, originalName = "javax.faces.lifecycle.Lifecycle")
public class Lifecycle_Instrumentation {
    @Trace
    public void execute(FacesContext context) throws FacesException {
        setMetricName("execute");
        Weaver.callOriginal();
    }

    @Trace
    public void render(FacesContext context) throws FacesException {
        setMetricName("render");
        Weaver.callOriginal();
    }

    private void setMetricName(String methodName) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        if (transaction != null) {
            transaction.getTracedMethod().setMetricName("Java", this.getClass().getName(), methodName);
        }
    }
}
