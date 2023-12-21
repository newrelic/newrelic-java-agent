/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.web.reactive.result.method;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.web.reactive.SpringControllerUtility;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Weave(type = MatchType.ExactClass, originalName = "org.springframework.web.reactive.result.method.InvocableHandlerMethod")
public abstract class InvocableHandlerMethod_Instrumentation {

    abstract protected Method getBridgedMethod();

    abstract public Class<?> getBeanType();

    @Trace
    public Mono<HandlerResult> invoke(
            ServerWebExchange exchange, BindingContext bindingContext, Object... providedArgs) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        if (transaction != null) {
            Class<?> controllerClass = getBeanType();
            Method controllerMethod = getBridgedMethod();

            //If this setting is false, attempt to name transactions the way the legacy point cut
            //named them
            boolean isEnhancedNaming = NewRelic.getAgent().getConfig().getValue("class_transformer.enhanced_spring_transaction_naming", false);

            String httpMethod = exchange.getRequest().getMethodValue();
            if (httpMethod != null) {
                httpMethod = httpMethod.toUpperCase();
            } else {
                httpMethod = "Unknown";
            }

            //Optimization - If a class doesn't have @Controller/@RestController directly on the controller class
            //the transaction is named in point cut style (with enhanced naming set to false)
            if (!isEnhancedNaming && !SpringControllerUtility.doesClassContainControllerAnnotations(controllerClass, false)) {
                transaction.getTracedMethod().setMetricName("Spring", "Java",
                        SpringControllerUtility.getControllerClassAndMethodString(controllerClass, controllerMethod, true));
                SpringControllerUtility.assignTransactionNameFromControllerAndMethod(transaction, controllerClass, controllerMethod);
            } else {
                String rootPath;
                String methodPath;

                //The list of elements to be used to set the traced method name. For point cut naming an element will be inserted at
                //the beginning of the list later
                List<String> tracedMethodElements = new ArrayList<>(Arrays.asList("Java",
                        SpringControllerUtility.getControllerClassAndMethodString(controllerClass, controllerMethod, true)));


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

                //Prefix "Spring" on the trace method metric if we're in point cut naming mode
                if (!isEnhancedNaming) {
                    tracedMethodElements.add(0, "Spring");
                }
                transaction.getTracedMethod().setMetricName(tracedMethodElements.toArray(new String[0]));
            }
        }

        return Weaver.callOriginal();
    }
}
