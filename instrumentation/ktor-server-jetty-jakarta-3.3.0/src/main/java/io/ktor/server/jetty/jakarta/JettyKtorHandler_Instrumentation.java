/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.server.jetty.jakarta;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.labs.instrumentation.ktor.jetty.jakarta.Utils;
import com.newrelic.labs.instrumentation.ktor.jetty.jakarta.JettyRequestHeaders;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;

@Weave(originalName = "io.ktor.server.jetty.jakarta.JettyKtorHandler")
public abstract class JettyKtorHandler_Instrumentation {

    @Trace(dispatcher = true)
    public boolean handle(Request request, Response response, Callback callback) {
        TracedMethod traced = NewRelic.getAgent().getTracedMethod();
        Transaction transaction = NewRelic.getAgent().getTransaction();
        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction();
        }
        HttpFields headers = request.getHeaders();
        JettyRequestHeaders jettyHeaders = new JettyRequestHeaders(headers);
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, jettyHeaders);
        String uri = request.getHttpURI().getPath();
        String method = request.getMethod();
        String contentType = headers != null ? headers.get(HttpHeader.CONTENT_TYPE) : null;
        if (uri != null) {
            traced.addCustomAttribute("Request-URI", uri);
        }
        if (method != null) {
            traced.addCustomAttribute("Request-Method", method);
        }
        if (contentType != null) {
            traced.addCustomAttribute("Content-Type", contentType);
        }
        String transactionName = Utils.getEnhancedTransactionName(uri, method);
        if (transactionName != null && !transactionName.isEmpty()) {
            transaction.setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "KtorJettyJakarta", transactionName);
        }
        return Weaver.callOriginal();
    }
}