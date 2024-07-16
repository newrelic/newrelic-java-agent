///*
// *
// *  * Copyright 2024 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package io.vertx.core.http.impl;
//
//import com.newrelic.api.agent.weaver.MatchType;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.Weaver;
//import com.nr.vertx.instrumentation.VertxUtil;
//import io.vertx.core.Handler;
//import io.vertx.core.http.HttpServerRequest;
//
//// This interface has HTTP/1 and HTTP/2 implementations
//@Weave(type = MatchType.Interface, originalName = "io.vertx.core.http.impl.HttpServerConnection")
//public abstract class HttpServerConnection_Instrumentation {
//    public HttpServerConnection handler(Handler<HttpServerRequest> handler) {
//        if (handler != null) {
//            // Wrap the request handler, so we can start the transaction and start tracking child threads
//            handler = VertxUtil.wrapRequestHandler(handler);
//        }
//        return Weaver.callOriginal();
//    }
//}
