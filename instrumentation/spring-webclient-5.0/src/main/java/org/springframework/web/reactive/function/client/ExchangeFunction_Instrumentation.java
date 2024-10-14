/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive.function.client;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring_webclient.Util;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Weave(type = MatchType.Interface, originalName = "org.springframework.web.reactive.function.client.ExchangeFunction")
public class ExchangeFunction_Instrumentation {

    public Mono<ClientResponse> exchange(ClientRequest request) {

        Segment segment = Util.startSegment();
        request = Util.addHeaders(request, segment);

        NewRelic.getAgent().getLogger().log(Level.INFO, "DT_DEBUG: In exchange(request). Outbound headers...");
        // Copied from the v6 HttpHeaders class
        String formattedHeaders = request.headers().entrySet().stream()
                .map(entry -> {
                    List<String> values = entry.getValue();
                    return entry.getKey() + ":" + (values.size() == 1 ?
                            "\"" + values.get(0) + "\"" :
                            values.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
                })
                .collect(Collectors.joining(", ", "[", "]"));
        NewRelic.getAgent().getLogger().log(Level.INFO, formattedHeaders);

        Mono<ClientResponse> response = Weaver.callOriginal();

        return Util.reportAsExternal(request, response, segment);
    }

}
