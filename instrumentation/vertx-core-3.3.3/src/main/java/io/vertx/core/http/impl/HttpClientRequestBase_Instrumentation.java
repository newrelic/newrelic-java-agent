/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.net.impl.ConnectionBase;

import java.net.UnknownHostException;

@Weave(originalName = "io.vertx.core.http.impl.HttpClientRequestBase")
abstract class HttpClientRequestBase_Instrumentation {

    @NewField
    public Segment segment;

    protected final HttpClientImpl client = Weaver.callOriginal();

    @Trace(async = true)
    public void handleException(Throwable t) {
        if (segment != null) {
            if (t instanceof UnknownHostException) {
                VertxCoreUtil.reportUnknownHost(segment);
            }
            final Token token = segment.getTransaction().getToken();
            segment.end();
            token.linkAndExpire();
        }
        Weaver.callOriginal();
    }

    @Trace(async = true)
    void handleResponse(HttpClientResponseImpl resp) {
        if (segment != null) {
            boolean isSsl = client.getSslHelper().isSSL();
            int port = -1;
            HttpConnection connection = connection();
            if (connection instanceof ConnectionBase) {
                port = ((ConnectionBase) connection).localAddress().port();
            }

            VertxCoreUtil.processResponse(segment, resp, resp.request().host, port, isSsl ? "https" : "http");
            final Token token = segment.getTransaction().getToken();
            segment.end();
            token.linkAndExpire();
        }

        Weaver.callOriginal();
    }

    public abstract MultiMap headers();

    public abstract HttpConnection connection();
}
