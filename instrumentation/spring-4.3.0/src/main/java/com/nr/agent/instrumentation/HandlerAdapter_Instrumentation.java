/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Weave(originalName = "org.springframework.web.servlet.HandlerAdapter", type = MatchType.Interface)
public class HandlerAdapter_Instrumentation {

    @Trace
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            String className;
            if (handler != null) {
                className = handler.getClass().getName();
                int idx = className.indexOf("$$EnhancerBy");
                if (idx > 0) {
                    className = className.substring(0, idx);
                }
                transaction.getTracedMethod().setMetricName("SpringController", className);
            } else {
                className = this.getClass().getName();
                int idx = className.indexOf("$$EnhancerBy");
                if (idx > 0) {
                    className = className.substring(0, idx);
                }
                transaction.getTracedMethod().setMetricName("SpringController", className, "handle");
            }
        }
        return Weaver.callOriginal();
    }
}