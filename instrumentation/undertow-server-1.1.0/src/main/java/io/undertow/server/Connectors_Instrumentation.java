/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.undertow.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.undertow.Util;

@Weave(type = MatchType.BaseClass, originalName = "io.undertow.server.Connectors")
public class Connectors_Instrumentation {
    @Trace(dispatcher=true)
    public static void executeRootHandler(HttpHandler handler, HttpServerExchange exchange) {
        Transaction transaction = NewRelic.getAgent().getTransaction();

        // We use a constant requestPath for the transaction name in case none of the downstream weaved
        // classes get invoked to rename it. If we used the actual requestPath from the exchange, it could result
        // in a bunch of unique transactions names being produced.
        Util.addTransactionNamedByParameter(Util.NamedBySource.ConnectorInstrumentation);
        transaction.setTransactionName(TransactionNamePriority.REQUEST_URI, false, "Undertow",
                Util.createTransactionName("{connectors_placeholder_name}/", exchange.getRequestMethod().toString()));

        Weaver.callOriginal();
    }
}
