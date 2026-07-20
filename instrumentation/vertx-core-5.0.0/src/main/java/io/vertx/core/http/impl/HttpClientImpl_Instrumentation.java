/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.RequestOptions;

@Weave(originalName = "io.vertx.core.http.impl.HttpClientImpl")
public class HttpClientImpl_Instrumentation {

    public Future<HttpClientRequest> request(RequestOptions options) {
        Future<HttpClientRequest> result = Weaver.callOriginal();
        if (result != null && AgentBridge.getAgent().getTransaction(false) != null) {
            Token token = NewRelic.getAgent().getTransaction().getToken();
            VertxCoreUtil.attachRequestResultHandler(result, token);
        }
        return result;
    }
}
