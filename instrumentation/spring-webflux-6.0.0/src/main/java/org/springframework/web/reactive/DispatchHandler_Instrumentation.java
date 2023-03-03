/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.web.reactive;

import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring.reactive.Util;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Based on OpenTelemetry instrumentation
// https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/spring/spring-webflux-5.0/javaagent/src/main/java/io/opentelemetry/javaagent/instrumentation/spring/webflux/server/DispatcherHandlerAdvice.java
@Weave(type =MatchType.ExactClass, originalName = "org.springframework.web.reactive.DispatcherHandler")
public class DispatchHandler_Instrumentation {
    @Trace
    public Mono<Void> handle(ServerWebExchange exchange) {
        final Token token = exchange == null ? null : exchange.getAttribute(Util.NR_TOKEN);

        Mono<Void> original = Weaver.callOriginal();

        if (token != null) {
            return Util.setTransactionToken(original, token);
        }
        return original;
    }
}
