/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.server;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring.reactive.Util;
import org.springframework.http.ReactiveHttpOutputMessage_Instrumentation;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.Map;

@Weave(originalName = "org.springframework.web.server.ServerWebExchange", type = MatchType.Interface)
public class ServerWebExchange_Instrumentation {

    public ServerHttpResponse getResponse() {
        final ServerHttpResponse response = Weaver.callOriginal();
        if (response instanceof ReactiveHttpOutputMessage_Instrumentation) {
            ReactiveHttpOutputMessage_Instrumentation reactiveResponse = (ReactiveHttpOutputMessage_Instrumentation) response;
            reactiveResponse.token = getAttribute(Util.NR_TOKEN);
        }
        return response;
    }

    public <T> T getAttribute(String name) {
        return Weaver.callOriginal();
    }

    public Map<String, Object> getAttributes() {
        return Weaver.callOriginal();
    }
}
