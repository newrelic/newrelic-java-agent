/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.glassfish.jersey.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.jersey.client.InboundWrapper;
import com.nr.instrumentation.jersey.client.JerseyClientUtils;
import com.nr.instrumentation.jersey.client.OutboundWrapper;

import jakarta.ws.rs.ProcessingException;
import java.net.UnknownHostException;

@Weave(type = MatchType.ExactClass, originalName = "org.glassfish.jersey.client.ClientRuntime")
abstract class ClientRuntime_Instrumentation {

    @Trace(leaf = true)
    public ClientResponse invoke(final ClientRequest request) throws Exception {
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(new OutboundWrapper(request));

        ClientResponse response;

        try {
            response = Weaver.callOriginal();
        } catch (ProcessingException ex) {
            if (ex.getCause() instanceof UnknownHostException) {
                NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                        .library(JerseyClientUtils.JERSEY_CLIENT)
                        .uri(JerseyClientUtils.UNKNOWN_HOST_URI)
                        .procedure(JerseyClientUtils.FAILED)
                        .noInboundHeaders()
                        .build());
            }
            throw ex;
        }

        NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                .library(JerseyClientUtils.JERSEY_CLIENT)
                .uri(request.getUri())
                .procedure(request.getMethod())
                .inboundHeaders(new InboundWrapper(response))
                .build());

        return response;
    }

    Runnable createRunnableForAsyncProcessing(ClientRequest request, final ResponseCallback_Instrumentation callback) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            callback.segment = transaction.startSegment(JerseyClientUtils.JERSEY_SEGMENT_NAME);
            callback.segment.addOutboundRequestHeaders(new OutboundWrapper(request));
        }
        return Weaver.callOriginal();
    }
}