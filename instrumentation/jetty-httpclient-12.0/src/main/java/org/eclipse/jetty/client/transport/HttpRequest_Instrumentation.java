/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.client.transport;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jettyhttpclient120.NRListener;
import com.nr.agent.instrumentation.jettyhttpclient120.OutboundWrapper;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.net.URI;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.client.transport.HttpRequest")
public abstract class HttpRequest_Instrumentation {

//    @Trace(leaf = true)
    @Trace
    public void send(Response.CompleteListener listener) {
        com.newrelic.agent.bridge.Transaction txn =
                AgentBridge.getAgent().getTransaction(false);

        if (txn != null) {
            // (Request)(Object) this: weave class is not declared as implementing Request,
            // but the real HttpRequest does — double-cast bypasses compile-time check.
            NewRelic.getAgent()
                    .getTracedMethod()
                    .addOutboundRequestHeaders(new OutboundWrapper((Request) (Object) this));

            URI requestURI = getURI();
            Segment segment = txn.startSegment("External/JettyHttpClient/send");
            if (segment != null) {
                NRListener nrListener = new NRListener(segment, requestURI);
                onRequestFailure(nrListener);
                onResponseSuccess(nrListener);
                onResponseFailure(nrListener);
            }
        }

        Weaver.callOriginal();
    }

    public abstract URI getURI();

    public abstract Request onRequestFailure(Request.FailureListener listener);

    public abstract Request onResponseSuccess(Response.SuccessListener listener);

    public abstract Request onResponseFailure(Response.FailureListener listener);
}
