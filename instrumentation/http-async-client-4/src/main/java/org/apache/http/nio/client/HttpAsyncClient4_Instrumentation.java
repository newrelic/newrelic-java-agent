/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.http.nio.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer_Instrumentation;
import com.nr.agent.instrumentation.httpasyncclient4.OutboundWrapper;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

import java.util.logging.Level;


@Weave(type = MatchType.Interface, originalName = "org.apache.http.nio.client.HttpAsyncClient")
public class HttpAsyncClient4_Instrumentation {

    /* Most common uses of the execute methods defined in the HttpAsyncClient interface should eventually call into this
     * particular execute signature, thus instrumenting only this one should result in fairly thorough instrumentation
     * coverage without double counting external calls. Possible exceptions include uses of execute in
     * CachingHttpAsyncClient and those defined in the HttpPipeliningClient interface.
     */
    public <T> Future<T> execute(
            HttpAsyncRequestProducer requestProducer,
            HttpAsyncResponseConsumer_Instrumentation<T> responseConsumer,
            HttpContext context,
            FutureCallback<T> callback) {
        try {
            URI uri = new URI(requestProducer.getTarget().toURI());
            String scheme = uri.getScheme();
            final Transaction txn = AgentBridge.getAgent().getTransaction(false);
            // only instrument HTTP or HTTPS calls
            final String lowerCaseScheme = scheme.toLowerCase();

            if (("http".equals(lowerCaseScheme) || "https".equals(lowerCaseScheme)) && txn != null) {
                try {
                    // Calls to generateRequest() don't appear to create a new HttpRequest object each time, but it does
                    // seem like other people could implement their own version that does. Something to be aware of.
                    final HttpRequest httpRequest = requestProducer.generateRequest();
                    Segment segment = txn.startSegment("External");
                    segment.addOutboundRequestHeaders(new OutboundWrapper(httpRequest));
                    // forward uri and segment to HttpAsyncResponseConsumer_Instrumentation
                    responseConsumer.segment = segment;
                    responseConsumer.uri = uri;
                } catch (IOException | HttpException e) {
                    AgentBridge.getAgent().getLogger().log(Level.FINEST, e, "Caught exception in HttpAsyncClient4 instrumentation: {0}");
                }
            }
        } catch (URISyntaxException uriSyntaxException) {
            // if Java can't parse the URI, HttpAsyncClient won't be able to either
            // let's just proceed without instrumentation
        }

        return Weaver.callOriginal();
    }
}
