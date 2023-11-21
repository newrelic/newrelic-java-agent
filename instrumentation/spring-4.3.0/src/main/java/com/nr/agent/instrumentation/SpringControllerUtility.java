/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.NewRelic;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.logging.Level;

public class SpringControllerUtility {

    public static String getPath(String rootPath, String methodPath, RequestMethod httpMethod) {
        StringBuilder fullPath = new StringBuilder();
        if (rootPath != null && !rootPath.isEmpty()) {
            if (rootPath.endsWith("/")) {
                fullPath.append(rootPath.substring(0, rootPath.length() - 1));
            } else {
                fullPath.append(rootPath);
            }
        }

        if (methodPath != null && !methodPath.isEmpty()) {
            if (!methodPath.startsWith("/")) {
                fullPath.append('/');
            }
            if (methodPath.endsWith("/")) {
                fullPath.append(methodPath.substring(0, methodPath.length() - 1));
            } else {
                fullPath.append(methodPath);
            }
        }

        if (httpMethod != null) {
            fullPath.append(" (").append(httpMethod.name()).append(')');
        }

        return fullPath.toString();
    }

    /**
     * Finds request mapping path. Returns first path of these two:
     *
     * 1) {@link RequestMapping#value()}
     * 2) {@link RequestMapping#path()}
     *
     * @return path or null if not found.
     */
    public static String getPathValue(String[] values, String[] path) {
        String result = null;
        if (values != null) {
            if (values.length > 0 && !values[0].contains("error.path")) {
                result = values[0];
            }
            if (result == null && path != null) {
                if (path.length > 0 && !path[0].contains("error.path")) {
                    result = path[0];
                }
            }
        }

        return result;
    }

    public static void processAnnotations(Transaction transaction, RequestMethod[] methods, String rootPath,
            String methodPath, Class<?> matchedAnnotationClass) {
        RequestMethod httpMethod = RequestMethod.GET;
        if (methods.length > 0) {
            httpMethod = methods[0];
        }

        if (rootPath == null && methodPath == null) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "No path was specified for SpringController {0}", matchedAnnotationClass.getName());
        } else {
            String fullPath = SpringControllerUtility.getPath(rootPath, methodPath, httpMethod);
            if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
                NewRelic.getAgent()
                        .getLogger()
                        .log(Level.FINEST, "SpringControllerUtility::processAnnotations (4.3.0): calling transaction.setTransactionName to [{0}] " +
                                        "with FRAMEWORK_HIGH and override true, txn {1}, ",
                                AgentBridge.getAgent().getTransaction().toString());
            }
            transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "SpringController",
                    fullPath);
        }
    }

}
