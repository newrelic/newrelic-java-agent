/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive.function.client;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring_webclient_60.Util;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "org.springframework.web.reactive.function.client.ExchangeFunction")
public class ExchangeFunction_Instrumentation {

    public Mono<ClientResponse> exchange(ClientRequest request) {
        Segment segment = Util.startSegment();
        request = Util.addHeaders(request, segment);

        NewRelic.getAgent().getLogger().log(Level.INFO, "DT_DEBUG: In exchange(request). Outbound headers...");
        NewRelic.getAgent().getLogger().log(Level.INFO, HttpHeaders.formatHeaders(request.headers()));

        Mono<ClientResponse> response = Weaver.callOriginal();

        return Util.reportAsExternal(request, response, segment);
    }

}
