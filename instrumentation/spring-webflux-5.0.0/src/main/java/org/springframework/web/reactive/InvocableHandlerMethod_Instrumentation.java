/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring.reactive.Util;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Weave(type = MatchType.ExactClass, originalName = "org.springframework.web.reactive.result.method.InvocableHandlerMethod")
public class InvocableHandlerMethod_Instrumentation {

    @Trace(async = true)
    public Mono<HandlerResult> invoke(ServerWebExchange exchange, BindingContext bindingContext, Object... providedArgs) {
        if (exchange != null) {
            Token token = exchange.getAttribute(Util.NR_TOKEN);
            if (token != null) {
                token.linkAndExpire();
            }
        }
        return Weaver.callOriginal();
    }
}
