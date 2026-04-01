/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.netty.http.client;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Weave(type = MatchType.BaseClass, originalName = "reactor.netty.http.client.HttpClientFinalizer")
abstract class HttpClientFinalizer_Instrumentation {

    Mono<HttpClientOperations> _connect() {
        Token token = NewRelic.getAgent().getTransaction().getToken();

        Mono<HttpClientOperations> connectMono = Weaver.callOriginal();

        if (token != null) {
            connectMono = connectMono.contextWrite(Context.of("newrelic-token", token));
        }

        return connectMono;
    }

}