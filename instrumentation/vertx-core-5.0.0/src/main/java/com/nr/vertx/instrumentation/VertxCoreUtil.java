/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
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
import com.newrelic.api.agent.weaver.Weaver;
import io.vertx.core.Completable;
import io.vertx.core.http.impl.HttpClientResponseImpl;

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

    public static void storeToken(Completable listener) {
        if (listener != null && AgentBridge.getAgent().getTransaction(false) != null) {
            final Token oldToken =
                    tokenMap.put(listener, NewRelic.getAgent().getTransaction().getToken());
            if (oldToken != null) {
                oldToken.expire();
            }
        }
    }

    public static void linkAndExpireToken(Completable listener) {
        if (listener != null) {
            final Token token = tokenMap.remove(listener);
            if (token != null) {
                token.linkAndExpire();
            }
        }
    }

    public static void expireToken(Completable listener) {
        if (listener != null) {
            final Token token = tokenMap.remove(listener);
            if (token != null) {
                token.expire();
            }
        }
    }

    public static void processResponse(Segment segment, HttpClientResponseImpl resp, String absoluteUri) {
        try {
            URI uri = new URI(absoluteUri);
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

    /**
     * Attaches an {@link AsyncRequestResultHandler} to the given request future.
     * Called from {@code HttpClientImpl_Instrumentation.request()} so that the
     * {@code onComplete} call happens outside the weave class body, calling
     * {@code onComplete} directly inside a weave method body causes an
     * {@code IncompatibleClassChangeError} at Vert.x startup due to interactions
     * between the weaver's class generation and the Vert.x future class hierarchy.
     */
    @SuppressWarnings("unchecked")
    public static void attachRequestResultHandler(io.vertx.core.Future requestFuture, Token token) {
        requestFuture.onComplete(new AsyncRequestResultHandler(token));
    }
}
