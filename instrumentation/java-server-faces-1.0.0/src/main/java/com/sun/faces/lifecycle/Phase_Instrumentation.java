/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.sun.faces.lifecycle;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import javax.faces.context.FacesContext;
import javax.faces.lifecycle.Lifecycle;
import java.util.ListIterator;

@Weave(type = MatchType.ExactClass, originalName = "com.sun.faces.lifecycle.Phase")
public class Phase_Instrumentation {
    @Trace
    public void doPhase(FacesContext context, Lifecycle lifecycle, ListIterator phases) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            transaction.getTracedMethod().setMetricName("Custom", this.getClass().getName(), "doPhase");
        }
        Weaver.callOriginal();
    }
}