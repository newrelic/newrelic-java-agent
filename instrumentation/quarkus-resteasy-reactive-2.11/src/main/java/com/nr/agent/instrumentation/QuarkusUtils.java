/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;

public class QuarkusUtils {
    // Keys owned by the vertx-web-3.8.3 module — used to bridge into its transaction and
    // suppress its name in favor of the JAX-RS route template.
    public static final String VERTX_TOKEN_KEY = "newrelic-token";
    public static final String VERTX_PATH_KEY = "newrelic-path";

    public static void setTransactionName(RuntimeResource resource) {
        if (resource != null) {
            URITemplate classPath = resource.getClassPath();
            URITemplate path = resource.getPath();
            String httpMethod = resource.getHttpMethod();

            StringBuilder sb = new StringBuilder();
            if (classPath != null && classPath.template != null && !classPath.template.isEmpty()) {
                sb.append(classPath.template);
            }
            if (path != null && path.template != null && !path.template.isEmpty()) {
                sb.append(path.template);
            }
            if (httpMethod != null && !httpMethod.isEmpty()) {
                sb.append(" (").append(httpMethod).append(")");
            }

            // We use CUSTOM_LOW here to prevent the Vertx module from overwriting the transaction name
            // when a quarkus error occurs. CUSTOM_LOW is priority 11; FRAMEWORK_HIGH is 10.
            if (sb.length() > 0) {
                NewRelic.getAgent().getTransaction()
                        .setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "Quarkus", sb.toString());
            }
        }
    }
}
