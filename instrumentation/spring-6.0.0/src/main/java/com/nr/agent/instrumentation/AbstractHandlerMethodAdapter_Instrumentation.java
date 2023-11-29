/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;

@Weave(type = MatchType.BaseClass, originalName = "org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter")
public class AbstractHandlerMethodAdapter_Instrumentation {
    @Trace
    protected ModelAndView handleInternal(HttpServletRequest request,
            HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        if (transaction != null) {
            Class<?> controllerClass = handlerMethod.getBeanType();
            Method controllerMethod = handlerMethod.getMethod();

            String httpMethod = request.getMethod();
            if (httpMethod != null) {
                httpMethod = httpMethod.toUpperCase();
            } else {
                httpMethod = "Unknown";
            }

            String rootPath;
            String methodPath;

            //Set the metric name for this @Trace to the target controller method
            SpringControllerUtility.setTracedMethodMetricName(transaction, controllerClass, controllerMethod);

            //Handle typical controller methods with class and method annotations. Those annotations
            //can come from implemented interfaces, extended controller classes or be on the controller class itself.
            //Note that only RequestMapping mapping annotations can apply to a class (not Get/Post/etc)
            rootPath = SpringControllerUtility.retrieveRootMappingPathFromController(controllerClass);

            //Retrieve the mapping that applies to the target method
            methodPath = SpringControllerUtility.retrieveMappingPathFromHandlerMethod(controllerMethod, httpMethod);

            if (rootPath != null || methodPath != null) {
                SpringControllerUtility.assignTransactionNameFromControllerAndMethodRoutes(transaction, httpMethod, rootPath, methodPath);
            } else {
                //Name based on class + method
                SpringControllerUtility.assignTransactionNameFromControllerAndMethod(transaction, controllerClass, controllerMethod);
            }
        }

        return Weaver.callOriginal();
    }
}
