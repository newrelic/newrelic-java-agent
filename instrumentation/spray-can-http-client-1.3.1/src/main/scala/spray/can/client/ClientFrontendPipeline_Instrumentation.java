/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.can.client;

import akka.io.Tcp;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spray.can.client.InboundHttpHeaders;
import spray.can.Http;
import spray.http.HttpMessageEnd;
import spray.http.HttpRequestPart;
import spray.http.HttpRequest_Instrumentation;
import spray.http.HttpResponse;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.logging.Level;

@Weave(originalName = "spray.can.client.ClientFrontend$$anon$1$$anon$2$$anonfun$2")
public class ClientFrontendPipeline_Instrumentation {

    private final ClientFrontendOuterPipeline_Instrumentation $outer = Weaver.callOriginal();

    @Trace(async = true)
    public final void apply(final Tcp.Event event) {
        if (event instanceof Http.MessageEvent && ((Http.MessageEvent) event).ev() instanceof HttpMessageEnd) {
            final HttpMessageEnd httpMessageEnd = (HttpMessageEnd) ((Http.MessageEvent) event).ev();
            if (this.$outer.spray$can$client$ClientFrontend$$anon$$anon$$openRequests().size() == 1) {
                RequestRecord_Instrumentation requestRecord = this.$outer.spray$can$client$ClientFrontend$$anon$$anon$$openRequests().head();
                HttpRequestPart request = requestRecord.request();
                if (request instanceof HttpRequest_Instrumentation) {
                    Segment segment = ((HttpRequest_Instrumentation) request).segment;

                    try {
                        boolean isSSL = ((HttpRequest_Instrumentation) request).isSSL;
                        InetSocketAddress hostAndPort = ((HttpRequest_Instrumentation) request).remoteAddress;

                        segment.reportAsExternal(HttpParameters
                                .library("SprayCanClient")
                                .uri(new URI((isSSL ? "https" : "http"), null, hostAndPort.getHostName(), hostAndPort.getPort(),
                                        ((HttpRequest_Instrumentation) request).uri().path().toString(), null, null))
                                .procedure("connection")
                                .inboundHeaders(new InboundHttpHeaders(((HttpResponse) httpMessageEnd).headers()))
                                .build());
                        segment.end();
                    } catch (Exception e) {
                        NewRelic.getAgent().getLogger().log(Level.FINE, e, "Unable to record SprayCanClient externals");
                    }
                }
            }
        }

        Weaver.callOriginal();
    }

}
