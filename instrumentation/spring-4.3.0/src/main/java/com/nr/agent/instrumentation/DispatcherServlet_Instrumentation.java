/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;

@Weave(originalName = "org.springframework.web.servlet.DispatcherServlet", type = MatchType.ExactClass)
public class DispatcherServlet_Instrumentation {
    @Trace
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        if (transaction != null) {
            transaction.getTracedMethod().setMetricName("Java", this.getClass().getName(), "doDispatch");
        }

        Weaver.callOriginal();
    }

    @Trace
    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        if (transaction != null) {
            transaction.getTracedMethod().setMetricName("SpringView", "Java", this.getClass().getName(), "render");
        }

        Weaver.callOriginal();
    }
    protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) throws Exception {
        NewRelic.noticeError(ex);
        return Weaver.callOriginal();
    }

    private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
            HandlerExecutionChain mappedHandler, Exception ex) throws Exception {
        NewRelic.noticeError(ex);
        Weaver.callOriginal();
    }

}