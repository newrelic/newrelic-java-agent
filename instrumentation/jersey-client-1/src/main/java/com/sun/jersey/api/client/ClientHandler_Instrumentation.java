/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.sun.jersey.api.client;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.jersey.client.InboundWrapper;
import com.nr.instrumentation.jersey.client.JerseyClientUtils;
import com.nr.instrumentation.jersey.client.OutboundWrapper;

@Weave(type = MatchType.Interface, originalName = "com.sun.jersey.api.client.ClientHandler")
public class ClientHandler_Instrumentation {

    @Trace(leaf = true)
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(new OutboundWrapper(request));

        ClientResponse response = Weaver.callOriginal();

        NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                .library(JerseyClientUtils.JERSEY_CLIENT)
                .uri(request.getURI())
                .procedure(request.getMethod())
                .inboundHeaders(new InboundWrapper(response))
                .build());

        return response;
    }
    
}
