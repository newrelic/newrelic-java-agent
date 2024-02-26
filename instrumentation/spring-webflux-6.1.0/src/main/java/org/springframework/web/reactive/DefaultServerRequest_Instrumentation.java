/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.instrumentation.spring.reactive.Util;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.Map;

@Weave(originalName = "org.springframework.web.reactive.function.server.DefaultServerRequest")
abstract class DefaultServerRequest_Instrumentation {

    DefaultServerRequest_Instrumentation(ServerWebExchange exchange, List<HttpMessageReader<?>> messageReaders) {
        final Token token = exchange == null ? null : exchange.getAttribute(Util.NR_TOKEN);
        if (token != null) {
            attributes().put(Util.NR_TOKEN, token);
        }
    }

    public abstract Map<String, Object> attributes();
}
