/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

import com.newrelic.agent.bridge.Transaction;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class JakartaWsRsApiHelper {

    public static final ThreadLocal<ResourcePath> subresourcePath = new ThreadLocal<ResourcePath>() {
        @Override
        protected ResourcePath initialValue() {
            return new ResourcePath(new LinkedBlockingQueue<String>(10));
        }
    };

    public static String getPath(String rootPath, String methodPath, String httpMethod) {
        StringBuilder fullPath = new StringBuilder();
        if (rootPath != null) {
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
            fullPath.append(" (").append(httpMethod).append(')');
        }

        return fullPath.toString();
    }

    public static class ResourcePath {

        public final Queue<String> pathQueue;
        public Transaction transaction;

        public ResourcePath(Queue<String> pathQueue) {
            this.pathQueue = pathQueue;
            this.transaction = null;
        }

    }
}
