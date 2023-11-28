/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.web.reactive.result.method;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
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
            String httpMethod = exchange.getRequest().getMethodValue();
            String rootPath = null;
            String methodPath = null;

            //Set the metric name for this @Trace to the target controller method
            SpringControllerUtility.setTracedMethodMetricName(transaction, controllerClass, controllerMethod);

            //Handle typical controller methods with class and method annotations. Those annotations
            //can come from implemented interfaces, extended controller classes or be on the controller class itself.
            //Note that only RequestMapping mapping annotations can apply to a class (not Get/Post/etc)
            rootPath = SpringControllerUtility.retrieveRootMappingPathFromController(controllerClass);

            //Retrieve the mapping that applies to the target method
            methodPath = SpringControllerUtility.retrieveMappingPathFromHandlerMethod(controllerMethod);

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
