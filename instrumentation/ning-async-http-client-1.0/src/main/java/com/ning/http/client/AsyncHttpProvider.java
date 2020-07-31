/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ning.http.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.asynchttpclient.NRAsyncHandler;
import com.nr.agent.instrumentation.asynchttpclient.OutboundWrapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

/**
 * Instrumentation for the provider interface.
 */
@Weave(type = MatchType.Interface)
public class AsyncHttpProvider {

    public <T> Future<T> execute(Request request, NRAsyncHandler<T> handler) throws IOException {

        URI uri = null;
        try {
            uri = new URI(request.getUrl());
            String scheme = uri.getScheme();

            // only instrument HTTP or HTTPS calls
            if ((scheme == null || scheme.equals("http") || scheme.equals("https"))
                && null != AgentBridge.getAgent().getTransaction(false)
                && AgentBridge.getAgent().getTransaction().isStarted()) {
                // start the timer
                Segment segment = NewRelic.getAgent().getTransaction().startSegment("execute");
                segment.addOutboundRequestHeaders(new OutboundWrapper(request));

                handler.segment = segment;
                handler.uri = uri;
            }
        } catch (URISyntaxException uriSyntaxException) {
            // if Java can't parse the URI, ning won't be able to either
            // let's just proceed without instrumentation
        }

        // proceed
        return Weaver.callOriginal();
    }
}
