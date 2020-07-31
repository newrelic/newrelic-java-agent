/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.asynchttpclient;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import play.shaded.ahc.org.asynchttpclient.ListenableFuture;
import play.shaded.ahc.org.asynchttpclient.Request;

import java.net.URI;
import java.net.URISyntaxException;

@Weave(type = MatchType.Interface, originalName = "play.shaded.ahc.org.asynchttpclient.AsyncHttpClient")
public class AsyncHttpClient_Instrumentation {

    public <T> ListenableFuture<T> executeRequest(Request request, AsyncHandler_Instrumentation<T> handler) {
        URI uri = null;
        try {
            uri = new URI(request.getUrl());
            String scheme = uri.getScheme().toLowerCase();

            // only instrument HTTP or HTTPS calls
            if ("http".equals(scheme) || "https".equals(scheme)) {
                Transaction txn = AgentBridge.getAgent().getTransaction(false);
                if (txn != null) {
                    handler.token = txn.getToken();
                }
            }
        } catch (URISyntaxException uriSyntaxException) {
            // if Java can't parse the URI, asynchttpclient won't be able to either
            // let's just proceed without instrumentation
        }

        return Weaver.callOriginal();
    }

}
