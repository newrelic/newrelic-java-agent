 /*
  *
  *  * Copyright 2026 New Relic Corporation. All rights reserved.
  *  * SPDX-License-Identifier: Apache-2.0
  *
  */

 package com.newrelic.labs.instrumentation.ktor.jetty;

import io.ktor.server.application.Application;

public class Utils {

    public static String getApplicationName(Application app) {
        if (app != null) {
            return app.getClass().getSimpleName();
        }
        return "Unknown";
    }
    
    public static String getTransactionName(String uri, String method) {
        StringBuilder sb = new StringBuilder();
        
        if (uri != null) {
            if (uri.startsWith("/")) {
                uri = uri.substring(1);
            }
            if (uri.isEmpty()) {
                sb.append("Root");
            } else {
                sb.append(uri);
            }
        } else {
            sb.append("Unknown");
        }
        
        if (method != null && !method.isEmpty()) {
            sb.append(" - {");
            sb.append(method);
            sb.append("}");
        }
        
        return sb.toString();
    }
    
    public static String getTransactionNameFromConnectionPoint(Object connectionPoint) {
        // For now, return a simple transaction name based on connection point
        // This would need to be enhanced based on actual RequestConnectionPoint implementation
        if (connectionPoint != null) {
            return connectionPoint.toString();
        }
        return "JettyConnection";
    }
}