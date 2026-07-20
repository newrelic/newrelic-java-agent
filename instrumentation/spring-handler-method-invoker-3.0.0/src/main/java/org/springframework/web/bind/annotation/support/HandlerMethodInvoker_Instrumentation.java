/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.web.bind.annotation.support;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;

@Weave(originalName = "org.springframework.web.bind.annotation.support.HandlerMethodInvoker", type = MatchType.ExactClass)
public class HandlerMethodInvoker_Instrumentation {
    @Trace
    public Object invokeHandlerMethod(Method handlerMethod, Object handler,
                                      NativeWebRequest webRequest, ExtendedModelMap implicitModel) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            String methodName = handlerMethod.getName();
            String controllerName = handler.getClass().getName();

            int idx = controllerName.indexOf("$$EnhancerBy");
            if (idx > 0) {
                controllerName = controllerName.substring(0, idx);
            }

            transaction.getTracedMethod().setMetricName("Spring", "Java", controllerName, methodName);
            transaction.setTransactionName(TransactionNamePriority.FRAMEWORK, true,
                    "SpringController", "/" + controllerName + "/" + methodName);
        }

        return Weaver.callOriginal();
    }
}
