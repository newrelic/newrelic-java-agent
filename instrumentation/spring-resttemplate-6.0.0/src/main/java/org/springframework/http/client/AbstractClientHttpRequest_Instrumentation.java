/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.http.client;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.spring.OutboundHeadersWrapper;
import org.springframework.http.HttpHeaders;

import java.io.IOException;

/**
 * All Spring HTTP transports (HttpURLConnection, Apache HttpClient, Jetty, OkHttp, Reactor Netty)
 * extend AbstractClientHttpRequest and call executeInternal() to send the request.
 */
@Weave(type = MatchType.BaseClass, originalName = "org.springframework.http.client.AbstractClientHttpRequest")
public abstract class AbstractClientHttpRequest_Instrumentation {

    protected final ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
        OutboundHeadersWrapper.addOutboundHeaders(headers);

        return Weaver.callOriginal();
    }
}