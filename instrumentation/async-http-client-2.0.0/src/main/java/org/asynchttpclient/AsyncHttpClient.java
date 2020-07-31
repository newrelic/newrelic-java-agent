/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.asynchttpclient;

import java.net.URI;
import java.net.URISyntaxException;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.asynchttpclient.NRAsyncHandler;
import com.nr.agent.instrumentation.asynchttpclient.OutboundWrapper;

/**
 * Instrumentation for the provider interface.
 */
@Weave(type = MatchType.Interface)
public class AsyncHttpClient {

    public <T> ListenableFuture<T> executeRequest(Request request, NRAsyncHandler<T> handler) {

        URI uri = null;

        try {
            uri = new URI(request.getUrl());

            String scheme = uri.getScheme();

            // only instrument HTTP or HTTPS calls
            if ((scheme == null || scheme.equals("http") || scheme.equals("https"))
                    && null != AgentBridge.getAgent().getTransaction(false)
                    && AgentBridge.getAgent().getTransaction().isStarted()) {
                // start the activity
                Segment segment = AgentBridge.getAgent().getTransaction().startSegment("External");
                if (null != segment) {
                    segment.addOutboundRequestHeaders(new OutboundWrapper(request));

                    handler.uri = uri;
                    handler.segment = segment;
                }
            }
        } catch (URISyntaxException uriSyntaxException) {
            // if Java can't parse the URI, asynchttpclient won't be able to either
            // let's just proceed without instrumentation
        }
        // proceed
        return Weaver.callOriginal();
    }
}