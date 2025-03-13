/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.undertow;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;
import io.undertow.server.HttpServerExchange;

public class Util {
    public enum NamedBySource {
        ConnectorInstrumentation,
        RoutingHandler,
        PathTemplateHandler,
        PathTemplatePredicate,
    }

    public static void addTransactionNamedByParameter(NamedBySource source)  {
        NewRelic.addCustomParameter("Transaction-Named-By", source.toString());
    }

    public static String createTransactionName(String requestPath, String method) {
        return (requestPath != null ? requestPath : "(Unknown)") + " (" + (method != null ? method : "Unknown") + ")";
    }

    public static void setWebRequestAndResponse(HttpServerExchange exchange) {
        Transaction transaction = NewRelic.getAgent().getTransaction();
        transaction.setWebRequest(new RequestWrapper(exchange));
        transaction.setWebResponse(new ResponseWrapper(exchange));
    }
}
