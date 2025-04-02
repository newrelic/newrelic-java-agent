/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut.netty_43;

import io.micronaut.web.router.RouteMatch;

import java.util.Map;

public class Utils {

    public static void decorateWithRoute(RouteMatch<?> routeMatch) {
//		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
//		if(routeMatch instanceof BasicObjectRouteMatch) {
//			BasicObjectRouteMatch objMatch = (BasicObjectRouteMatch)routeMatch;
//			Class<?> declaringClass = objMatch.getDeclaringType();
//			traced.setMetricName("Custom","Micronaut","Netty","Route","Object", declaringClass.getSimpleName());
//			traced.addCustomAttribute("Declaring-Class", declaringClass.getName());
//		} else if(routeMatch instanceof UriRouteMatch) {
//			UriRouteMatch<?,?> uriRouteMatch = (UriRouteMatch<?, ?>)routeMatch;
//			
//			
//			
//			String uri = uriRouteMatch.getUri();
//			UriMatchTemplate matchTemplate = uriRouteMatch.getRoute().getUriMatchTemplate();
//			String pathString = matchTemplate != null ? matchTemplate.toPathString() : null;
//			
//			String uriTemplate = uriRouteMatch.toString();
//			String methodName = uriRouteMatch.getMethodName();
//			String name = uriRouteMatch.getName();
//			
//			HashMap<String, Object> attributes = new HashMap<String, Object>();
//			addAttribute(attributes, "PathString", pathString);
//			addAttribute(attributes, "Method-Name", methodName);
//			addAttribute(attributes, "URI", uri);
//			addAttribute(attributes, "URITemplate", uriTemplate);
//			addAttribute(attributes, "Name", name);
//			traced.addCustomAttributes(attributes);
//			
//			traced.setMetricName("Custom","Micronaut","Netty","Route","URI");
//		}
    }

    public static void addAttribute(Map<String, Object> attributes, String key, Object value) {
        if (attributes != null && key != null && !key.isEmpty() && value != null) {
            attributes.put(key, value);
        }
    }
}
