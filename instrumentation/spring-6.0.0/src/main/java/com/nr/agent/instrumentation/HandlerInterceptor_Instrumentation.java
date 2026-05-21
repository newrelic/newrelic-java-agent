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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Weave(type = MatchType.Interface, originalName = "org.springframework.web.servlet.HandlerInterceptor")
public class HandlerInterceptor_Instrumentation {
    @Trace
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        setMetricName("preHandle");
        return Weaver.callOriginal();
    }

    @Trace
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) {
        setMetricName("postHandle");
        Weaver.callOriginal();
    }

    @Trace
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        setMetricName("afterCompletion");
        Weaver.callOriginal();
    }

    private void setMetricName(String methodName) {
        // This allows us to get a rollup metric name in the form: Spring/HandlerInterceptor
        // and a segment name like:  Spring/Java/com.foo.MyClass/preHandle
        // This matches the legacy point cut behavior
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            com.newrelic.api.agent.TracedMethod tracedMethod = transaction.getTracedMethod();
            if (tracedMethod != null) {
                String className = getClass().getName();
                // Sets both metric name and segment name to the class-specific value
                tracedMethod.setMetricName("Spring", "Java", className, methodName);
                // Adds the rollup metric as an additional unscoped metric
                tracedMethod.addRollupMetricName("Spring", "HandlerInterceptor");
            }
        }
    }
}
