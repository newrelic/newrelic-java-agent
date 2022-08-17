/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.ws.rs.*;

import java.util.Queue;
import java.util.logging.Level;

@WeaveWithAnnotation(annotationClasses = { "jakarta.ws.rs.Path" }, type = MatchType.Interface)
public class JakartaWsRsApi_Instrumentation {

    @WeaveWithAnnotation(annotationClasses = { "jakarta.ws.rs.PUT", "jakarta.ws.rs.POST", "jakarta.ws.rs.GET",
            "jakarta.ws.rs.DELETE", "jakarta.ws.rs.HEAD", "jakarta.ws.rs.OPTIONS", "jakarta.ws.rs.PATCH" })
    @WeaveIntoAllMethods
    @Trace(dispatcher = true)
    private static void instrumentation() {
        Transaction transaction = AgentBridge.getAgent().getWeakRefTransaction(false);
        if (transaction != null) {
            JakartaWsRsApiHelper.ResourcePath resourcePath = JakartaWsRsApiHelper.subresourcePath.get();
            if (!transaction.equals(resourcePath.transaction)) {
                resourcePath.pathQueue.clear();
                resourcePath.transaction = transaction;
            }

            String rootPath = null;
            Path rootPathAnnotation = Weaver.getClassAnnotation(Path.class);
            if (rootPathAnnotation != null) {
                rootPath = rootPathAnnotation.value();
            }
            String methodPath = null;
            Path methodPathAnnotation = Weaver.getMethodAnnotation(Path.class);
            if (methodPathAnnotation != null) {
                methodPath = methodPathAnnotation.value();
            }

            String httpMethod = null;
             /* Unfortunately we have to do this in a big if/else block because we can't currently support variables passed
             into the Weaver.getMethodAnnotation method. We might be able to make an improvement to handle this in the future */
            if (Weaver.getMethodAnnotation(PUT.class) != null) {
                httpMethod = PUT.class.getSimpleName();
            } else if (Weaver.getMethodAnnotation(POST.class) != null) {
                httpMethod = POST.class.getSimpleName();
            } else if (Weaver.getMethodAnnotation(GET.class) != null) {
                httpMethod = GET.class.getSimpleName();
            } else if (Weaver.getMethodAnnotation(DELETE.class) != null) {
                httpMethod = DELETE.class.getSimpleName();
            } else if (Weaver.getMethodAnnotation(HEAD.class) != null) {
                httpMethod = HEAD.class.getSimpleName();
            } else if (Weaver.getMethodAnnotation(OPTIONS.class) != null) {
                httpMethod = OPTIONS.class.getSimpleName();
            } else {
                // PATCH annotation was added later so may not be available
                try {
                    if (Weaver.getMethodAnnotation(PATCH.class) != null) {
                        httpMethod = PATCH.class.getSimpleName();
                    }
                } catch (NoClassDefFoundError e) {
                }
            }

            if (httpMethod != null) {
                Queue<String> subresourcePath = resourcePath.pathQueue;

                String fullPath = JakartaWsRsApiHelper.getPath(rootPath, methodPath, httpMethod);
                subresourcePath.offer(fullPath);

                StringBuilder txNameBuilder = new StringBuilder();
                String pathPart;
                while ((pathPart = subresourcePath.poll()) != null) {
                    txNameBuilder.append(pathPart);
                }

                transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "RestWebService",
                        txNameBuilder.toString());
            } else {
                String fullPath = JakartaWsRsApiHelper.getPath(rootPath, methodPath, null);
                if (!resourcePath.pathQueue.offer(fullPath)) {
                    AgentBridge.getAgent().getLogger().log(Level.FINE, "JAX-RS Subresource naming queue is full.");
                    resourcePath.pathQueue.clear();
                }
            }
        }
    }
}
