/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.Weaver;
import io.vertx.core.Handler;
import io.vertx.core.http.impl.HttpClientResponseImpl;
import io.vertx.core.impl.future.Listener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class VertxCoreUtil {

    private VertxCoreUtil() {
    }

    private static final Map<Object, Token> tokenMap = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static final String VERTX_CLIENT = "Vertx-Client";
    public static final String END = "handleResponse";

    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    public static void storeToken(Listener handler) {
        if (handler != null && AgentBridge.getAgent().getTransaction(false) != null) {
            tokenMap.put(handler, NewRelic.getAgent().getTransaction().getToken());
        }
    }

    public static void linkAndExpireToken(Listener listener) {
        if (listener != null) {
            final Token token = tokenMap.remove(listener);
            if (token != null) {
                token.linkAndExpire();
            }
        }
    }

    public static void processResponse(Segment segment, HttpClientResponseImpl resp, String host, int port,
            String scheme) {
        try {
            URI uri = new URI(scheme, null, host, port, null, null, null);
            segment.reportAsExternal(HttpParameters.library(VERTX_CLIENT)
                                                   .uri(uri)
                                                   .procedure(END)
                                                   .inboundHeaders(new InboundWrapper(resp))
                                                   .status(resp.statusCode(), resp.statusMessage())
                                                   .build());
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
    }

    public static void reportUnknownHost(Segment segment) {
            segment.reportAsExternal(GenericParameters.library(VERTX_CLIENT)
                                                      .uri(UNKNOWN_HOST_URI)
                                                      .procedure(END)
                                                      .build());
    }
}
