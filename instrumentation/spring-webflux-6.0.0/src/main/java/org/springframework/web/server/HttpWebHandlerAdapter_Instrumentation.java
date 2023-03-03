/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring.reactive.Util;
import org.springframework.http.ReactiveHttpOutputMessage_Instrumentation;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

@Weave(type = MatchType.ExactClass, originalName = "org.springframework.web.server.adapter.HttpWebHandlerAdapter")
public class HttpWebHandlerAdapter_Instrumentation {

    protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
        final com.newrelic.agent.bridge.Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        final Token token = transaction == null ? null : transaction.getToken();

        if (response instanceof ReactiveHttpOutputMessage_Instrumentation) {
            ReactiveHttpOutputMessage_Instrumentation reactiveResponse = (ReactiveHttpOutputMessage_Instrumentation) response;
            reactiveResponse.token = token;
        }

        ServerWebExchange exchange = Weaver.callOriginal();
        if (token != null) {
            exchange.getAttributes().put(Util.NR_TOKEN, token);
        }
        return exchange;
    }
}
