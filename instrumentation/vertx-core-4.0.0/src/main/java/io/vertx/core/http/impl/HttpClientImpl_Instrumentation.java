/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.HttpClientRequestPromiseWrapper;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.future.PromiseInternal;
import io.vertx.core.net.SocketAddress;

@Weave(type = MatchType.BaseClass, originalName = "io.vertx.core.http.impl.HttpClientImpl")
public class HttpClientImpl_Instrumentation {
    private void request(
            HttpMethod method,
            SocketAddress peerAddress,
            SocketAddress server,
            String host,
            int port,
            Boolean useSSL,
            String requestURI,
            MultiMap headers,
            long timeout,
            Boolean followRedirects,
            PromiseInternal<HttpClientRequest> requestPromise) {
        requestPromise = new HttpClientRequestPromiseWrapper(requestPromise, NewRelic.getAgent().getTransaction().getToken());
        Weaver.callOriginal();
    }
}
