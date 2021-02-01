/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive.function.client;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring_webclient.Util;
import reactor.core.publisher.Mono;

@Weave(type = MatchType.Interface, originalName = "org.springframework.web.reactive.function.client.WebClient")
public class WebClient_Instrumentation {

    @Weave(type = MatchType.Interface, originalName = "org.springframework.web.reactive.function.client.WebClient$RequestHeadersSpec")
    public abstract static class RequestHeadersSpec_Instrumentation<S extends RequestHeadersSpec_Instrumentation<S>> {

        public Mono<ClientResponse> exchange() {

            Util.startExternalSegmentIfNeeded((WebClient.RequestHeadersSpec<?>) this);

            return Weaver.callOriginal();
        }
   }
}
