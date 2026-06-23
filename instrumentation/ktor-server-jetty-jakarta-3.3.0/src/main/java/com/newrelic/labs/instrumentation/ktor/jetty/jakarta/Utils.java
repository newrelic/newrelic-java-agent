/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.jetty.jakarta;

public class Utils {

    public static String getEnhancedTransactionName(String uri, String method) {
        StringBuilder sb = new StringBuilder();
        if (uri != null && !uri.isEmpty()) {
            String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            sb.append(path.isEmpty() ? "Root" : path);
        } else {
            sb.append("Unknown");
        }
        if (method != null && !method.isEmpty()) {
            sb.append(" - {").append(method).append("}");
        }
        return sb.toString();
    }
}
