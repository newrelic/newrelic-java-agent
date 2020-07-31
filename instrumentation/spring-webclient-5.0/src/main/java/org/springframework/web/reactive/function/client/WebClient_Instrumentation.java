/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive.function.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring_webclient.OutboundRequestWrapper;
import com.nr.agent.instrumentation.spring_webclient.Util;
import reactor.core.publisher.Mono;

import java.net.URI;

@Weave(type = MatchType.Interface, originalName = "org.springframework.web.reactive.function.client.WebClient")
public class WebClient_Instrumentation {

    @Weave(type = MatchType.Interface, originalName = "org.springframework.web.reactive.function.client.WebClient$RequestHeadersSpec")
    public abstract static class RequestHeadersSpec_Instrumentation<S extends RequestHeadersSpec_Instrumentation<S>> {

        public abstract S header(String headerName, String... headerValues);

        public Mono<ClientResponse> exchange() {

            Object thisTemp = this;
            URI uri = Util.getUri();
            Segment segment = null;

            if (thisTemp instanceof UriSpec_Instrumentation) {
                if (uri != null) {
                    String scheme = uri.getScheme();
                    if (scheme != null) {
                        final Transaction txn = AgentBridge.getAgent().getTransaction(false);
                        final String lowerCaseScheme = scheme.toLowerCase();
                        if (txn != null && ("http".equals(lowerCaseScheme) || "https".equals(lowerCaseScheme))) {
                            segment = NewRelic.getAgent().getTransaction().startSegment("WebClient.exchange");
                            segment.addOutboundRequestHeaders(
                                    new OutboundRequestWrapper((WebClient.RequestHeadersSpec) thisTemp));
                        }
                    }
                }
            }

            Mono<ClientResponse> response = Weaver.callOriginal();
            if (segment == null || uri == null) {
                return response;
            }
            return response.doAfterSuccessOrError(Util.reportAsExternal(segment));
        }
    }

    @Weave(type = MatchType.Interface, originalName = "org.springframework.web.reactive.function.client.WebClient$UriSpec")
    public static class UriSpec_Instrumentation<S extends WebClient.RequestHeadersSpec<?>> {

        public S uri(URI uri) {
            Util.setUri(uri);
            return Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.Interface, originalName = "org.springframework.web.reactive.function.client.WebClient$Builder")
    public static class Builder_Instrumentation {

        public WebClient.Builder baseUrl(String baseUrl) {
            Util.setUri(baseUrl);
            return Weaver.callOriginal();
        }
    }
}
