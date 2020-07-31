/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;

import com.newrelic.agent.bridge.Transaction;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.api.agent.weaver.Weaver;

@WeaveWithAnnotation(annotationClasses = {"org.springframework.stereotype.Controller"}, type = MatchType.ExactClass)
public class SpringController_Instrumentation {

    @WeaveWithAnnotation(annotationClasses = { "org.springframework.web.bind.annotation.RequestMapping" })
    @WeaveIntoAllMethods
    @Trace
    private static void instrumentation() {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            RequestMapping rootPathMapping = Weaver.getClassAnnotation(RequestMapping.class);
            RequestMapping methodPathMapping = Weaver.getMethodAnnotation(RequestMapping.class);

            String rootPath = SpringControllerUtility.getPathValue(rootPathMapping);
            String methodPath = SpringControllerUtility.getPathValue(methodPathMapping);

            RequestMethod httpMethod = RequestMethod.GET;
            RequestMethod[] methods = methodPathMapping.method();
            if (methods.length > 0) {
                httpMethod = methods[0];
            }

            if (rootPath == null && methodPath == null) {
                AgentBridge.getAgent().getLogger().log(Level.FINE, "No path was specified for SpringController {0}",
                        MethodHandles.lookup().lookupClass().getName());
            } else {
                String fullPath = SpringControllerUtility.getPath(rootPath, methodPath, httpMethod);
                transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "SpringController", fullPath);
            }
        }
    }

}
