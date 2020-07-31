/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
     * Finds request mapping path.
     *
     * @param annotation request mapping
     * @return path or null if not found.
     */
    public static String getPathValue(RequestMapping annotation) {
        String result = null;
        if (annotation != null) {
            String[] values = annotation.value();
            if (values.length > 0 && !values[0].contains("error.path")) {
                result = values[0];
            }
        }

        return result;
    }

}
