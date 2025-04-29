/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut.netty_2;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import io.micronaut.web.router.BasicObjectRouteMatch;
import io.micronaut.web.router.RouteMatch;
import io.micronaut.web.router.UriRouteMatch;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static void decorateWithRoute(RouteMatch<?> routeMatch) {
        Transaction transaction = NewRelic.getAgent().getTransaction();
        TracedMethod traced = NewRelic.getAgent().getTracedMethod();
        if (routeMatch instanceof BasicObjectRouteMatch) {
            BasicObjectRouteMatch objMatch = (BasicObjectRouteMatch) routeMatch;
            Class<?> declaringClass = objMatch.getDeclaringType();
            traced.setMetricName("Custom", "Micronaut", "Netty", "Route", "Object", declaringClass.getSimpleName());
            traced.addCustomAttribute("Declaring-Class", declaringClass.getName());
            transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "Routing", "Routing", "Object", declaringClass.getSimpleName());
        } else if (routeMatch instanceof UriRouteMatch) {
            UriRouteMatch<?, ?> uriRouteMatch = (UriRouteMatch<?, ?>) routeMatch;
            String uri = uriRouteMatch.getUri();
            String methodName = uriRouteMatch.getMethodName();
            String name = uriRouteMatch.getName();

            HashMap<String, Object> attributes = new HashMap<String, Object>();
            addAttribute(attributes, "Method-Name", methodName);
            addAttribute(attributes, "URI", uri);
            addAttribute(attributes, "Name", name);

            traced.setMetricName("Custom", "Micronaut", "Netty", "Route", "URI");
            transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "Routing", "Routing", "UriMatch", uri);
        }
    }

    public static void addAttribute(Map<String, Object> attributes, String key, Object value) {
        if (attributes != null && key != null && !key.isEmpty() && value != null) {
            attributes.put(key, value);
        }
    }
}
