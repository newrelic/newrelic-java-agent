/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.api.listener;

import java.net.URI;

import com.newrelic.api.agent.HttpParameters;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.endpoint.URIBuilder;
import org.mule.module.http.internal.domain.request.HttpRequestContext;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.mule3.MuleHttpConnectorRequest;

@Weave(type = MatchType.ExactClass, originalName = "org.mule.module.http.internal.listener.HttpRequestToMuleEvent")
public abstract class HttpRequestToMuleEvent_Instrumentation {

    @NewField
    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    /**
     * Verify the method body produces the same URI information as in module-3.7.
     */
    @Trace
    public static MuleEvent transform(final HttpRequestContext requestContext, final MuleContext muleContext,
                                      final FlowConstruct flowConstruct, Boolean parseRequest, String listenerPath) {
        MuleEvent event = Weaver.callOriginal();

        final MuleHttpConnectorRequest muleRequest = new MuleHttpConnectorRequest(event, requestContext);

        URI uri = resolveUriForNR(requestContext);
        NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                .library("MuleHTTP")
                .uri(uri)
                .procedure("writeResponse")
                .inboundHeaders(muleRequest)
                .build());

        final Transaction txn = AgentBridge.getAgent().getTransaction(false);
        txn.setWebRequest(muleRequest);

        final String txnName = event.getMessage().getInboundProperty("http.listener.path") + " (" + muleRequest.getMethod() + ")";
        txn.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "Mule/Connector", txnName);

        return event;
    }

    /**
     * resolveUri was introduced in Mule 3.7.0 and changed in Mule 3.8.0. It's better to re-implement a fixed version
     * and copy it into the 3.6 module and then re-use it going forward so transactions look consistent.
     */
    private static URI resolveUriForNR(final HttpRequestContext requestContext) {
        String host = requestContext.getRequest().getHeaderValue("host");
        if (host == null || host.isEmpty()) {
            return UNKNOWN_HOST_URI;
        } else {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setProtocol(requestContext.getScheme());
            uriBuilder.setHost(host);
            uriBuilder.setPath(requestContext.getRequest().getPath());
            return uriBuilder.getEndpoint().getUri();
        }
    }

}
