/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.glassfish.jersey.client;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.jersey.client.InboundWrapper;
import com.nr.instrumentation.jersey.client.JerseyClientUtils;
import org.glassfish.jersey.process.internal.RequestScope;

import jakarta.ws.rs.ProcessingException;
import java.net.UnknownHostException;

@Weave(originalName = "org.glassfish.jersey.client.ResponseCallback", type = MatchType.Interface)
class ResponseCallback_Instrumentation {
    @NewField
    public Segment segment;

    /**
     * Called when the client invocation was successfully completed with a response.
     *
     * @param response response data.
     * @param scope request processing scope.
     */
    @Trace(async = true, leaf = true)
    public void completed(ClientResponse response, RequestScope scope) {
        segment.getTransaction().getToken().linkAndExpire();

        segment.reportAsExternal(HttpParameters.library(JerseyClientUtils.JERSEY_CLIENT)
                .uri(response.getRequestContext().getUri())
                .procedure(response.getRequestContext().getMethod())
                .inboundHeaders(new InboundWrapper(response))
                .build());
        segment.end();
        segment = null;
        Weaver.callOriginal();
    }

    /**
     * Called when the invocation has failed for any reason.
     *
     * @param exception contains failure details.
     */
    @Trace(async = true, leaf = true)
    public void failed(ProcessingException exception) {
        segment.getTransaction().getToken().linkAndExpire();

        if (exception.getCause() instanceof UnknownHostException) {
            segment.reportAsExternal(HttpParameters
                    .library(JerseyClientUtils.JERSEY_CLIENT)
                    .uri(JerseyClientUtils.UNKNOWN_HOST_URI)
                    .procedure(JerseyClientUtils.FAILED)
                    .noInboundHeaders()
                    .build());
        }
        segment.end();
        segment = null;
        Weaver.callOriginal();
    }
}
