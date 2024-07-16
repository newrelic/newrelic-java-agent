/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.Weaver;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.impl.HttpClientResponseImpl;
import io.vertx.core.impl.future.Listener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class VertxCoreUtil {

    private VertxCoreUtil() {
    }

    private static final Map<Object, Token> tokenMap = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static final String VERTX_CLIENT = "Vertx-Client";
    public static final String END = "handleResponse";

    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    public static final AtomicInteger handleResponseCount = new AtomicInteger(1);


    public static void debug(Segment segment, HttpClientResponse resp, String callingClass) {
        Transaction transaction = NewRelic.getAgent().getTransaction();

        Logger logger = NewRelic.getAgent().getLogger();
        logger.log(Level.INFO, "[NettyDebug/VertxDebug] ======= " + callingClass + " COUNT : " + handleResponseCount.getAndIncrement() + " =======");

        logger.log(Level.INFO, "[NettyDebug/VertxDebug] segment: " + segment);

        logger.log(Level.INFO, "[NettyDebug/VertxDebug] HttpClientResponse: " + resp);

    }

    public static void storeToken(Listener listener) {
        if (listener != null && AgentBridge.getAgent().getTransaction(false) != null) {
            tokenMap.put(listener, NewRelic.getAgent().getTransaction().getToken());
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
