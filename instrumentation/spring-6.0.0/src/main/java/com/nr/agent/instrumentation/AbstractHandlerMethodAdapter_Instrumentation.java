/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Weave(type = MatchType.BaseClass, originalName = "org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter")
public class AbstractHandlerMethodAdapter_Instrumentation {
    @Trace
    protected ModelAndView handleInternal(HttpServletRequest request,
            HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        if (transaction != null) {
            Class<?> controllerClass = handlerMethod.getBeanType();
            Method controllerMethod = handlerMethod.getMethod();

            //If this setting is false, attempt to name transactions the way the legacy point cut
            //named them
            boolean isEnhancedNaming =
                    NewRelic.getAgent().getConfig().getValue("class_transformer.enhanced_spring_transaction_naming", false);

            String httpMethod = request.getMethod();
            if (httpMethod != null) {
                httpMethod = httpMethod.toUpperCase();
            } else {
                httpMethod = "Unknown";
            }

            //Optimization - If a class doesn't have @Controller/@RestController directly on the controller class
            //the transaction is named in point cut style (with enhanced naming set to false)
            if (!isEnhancedNaming && !SpringControllerUtility.doesClassContainControllerAnnotations(controllerClass, false)) {
                SpringControllerUtility.assignTransactionNameFromControllerAndMethod(transaction, controllerClass, controllerMethod);
            } else {    //Normal flow to check for annotations based on enhanced naming config flag
                String rootPath;
                String methodPath;

                //From this point, look for annotations on the class/method, respecting the config flag that controls if the
                //annotation has to exist directly on the class/method or can be inherited.

                //Handle typical controller methods with class and method annotations. Those annotations
                //can come from implemented interfaces, extended controller classes or be on the controller class itself.
                //Note that only RequestMapping mapping annotations can apply to a class (not Get/Post/etc)
                rootPath = SpringControllerUtility.retrieveRootMappingPathFromController(controllerClass, isEnhancedNaming);

                //Retrieve the mapping that applies to the target method
                methodPath = SpringControllerUtility.retrieveMappingPathFromHandlerMethod(controllerMethod, httpMethod, isEnhancedNaming);

                if (rootPath != null || methodPath != null) {
                    SpringControllerUtility.assignTransactionNameFromControllerAndMethodRoutes(transaction, httpMethod, rootPath, methodPath);
                } else {
                    //Name based on class + method
                    SpringControllerUtility.assignTransactionNameFromControllerAndMethod(transaction, controllerClass, controllerMethod);
                }
            }
            transaction.getTracedMethod().setMetricName("Spring", "Java",
                    SpringControllerUtility.getControllerClassAndMethodString(controllerClass, controllerMethod, true));
        }

        return Weaver.callOriginal();
    }
}
