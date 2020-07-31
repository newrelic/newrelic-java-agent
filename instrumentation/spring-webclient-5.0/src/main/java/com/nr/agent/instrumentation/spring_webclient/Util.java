/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spring_webclient;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public class Util {

    private static final String LIBRARY = "Spring-WebClient";
    private static final URI UNKNOWN_HOST = URI.create("UnknownHost");
    private static URI uri = null;

    public static BiConsumer<? super ClientResponse, Throwable> reportAsExternal(Segment segment) {
        return new BiConsumer<ClientResponse, Throwable>() {
            @Override
            public void accept(ClientResponse clientResponse, Throwable throwable) {
                try {
                    if (segment != null && uri != null) {
                        if (clientResponse != null) {
                            segment.reportAsExternal(HttpParameters
                                    .library(LIBRARY)
                                    .uri(uri)
                                    .procedure("exchange")
                                    .inboundHeaders(new InboundResponseWrapper(clientResponse))
                                    .build());
                        } else {
                            if (throwable instanceof UnknownHostException) {
                                segment.reportAsExternal(GenericParameters
                                        .library(LIBRARY)
                                        .uri(UNKNOWN_HOST)
                                        .procedure("failed")
                                        .build());
                            }
                        }
                    }
                    if (segment != null) {
                        segment.end();
                    }
                } catch (Throwable e) {
                    AgentBridge.getAgent()
                            .getLogger()
                            .log(Level.FINEST, e, "Caught exception in Spring-WebClient instrumentation: {0}");
                    AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
                }
            }
        };
    }

    public static void setUri(String uri) {
        Util.uri = URI.create(uri);
    }

    public static void setUri(URI uri) {
        Util.uri = uri;
    }

    public static URI getUri() {
       return Util.uri;
    }
}
