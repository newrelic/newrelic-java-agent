/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.asynchttpclient;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.asynchttpclient.OutboundWrapper;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Instrumentation for the provider interface.
 */
@Weave(type = MatchType.Interface, originalName = "org.asynchttpclient.AsyncHttpClient")
public class AsyncHttpClient_Instrumentation {

    public <T> ListenableFuture<T> executeRequest(Request request, AsyncHandler_Instrumentation<T> handler) {
        URI uri = null;
        try {
            uri = new URI(request.getUrl());
            Transaction txn = AgentBridge.getAgent().getTransaction(false);
            String scheme = uri.getScheme().toLowerCase();

            // only instrument HTTP or HTTPS calls
            if (("http".equals(scheme) || "https".equals(scheme)) && txn != null) {
                Segment segment = txn.startSegment("External");
                segment.addOutboundRequestHeaders(new OutboundWrapper(request));

                handler.uri = uri;
                handler.segment = segment;
            }
        } catch (URISyntaxException uriSyntaxException) {
            // if Java can't parse the URI, asynchttpclient won't be able to either
            // let's just proceed without instrumentation
        }

        return Weaver.callOriginal();
    }
}
