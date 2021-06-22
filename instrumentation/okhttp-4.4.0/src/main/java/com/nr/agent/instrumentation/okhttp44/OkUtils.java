/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp44;

import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.external.ExternalMetrics;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URI;
import java.net.UnknownHostException;

import static com.newrelic.agent.bridge.external.ExternalEvents.EXTERNAL_CALL_EVENT;
import static com.newrelic.agent.bridge.external.ExternalEvents.getExternalParametersMap;

public class OkUtils {

    private static final String LIBRARY = "OkHttp";

    private static final String PROCEDURE = "execute";

    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    /**
     * The original request is immutable, so internally the wrapper modifies a copy and saves it, which we need to
     * pull back out after adding the headers.
     */
    public static Request doOutboundCAT(Request request) {
        OutboundWrapper out = new OutboundWrapper(request);
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(out);
        return out.getRequestWithNRHeaders();
    }

    public static void handleUnknownHost(Exception e) {
        if (e instanceof UnknownHostException) {
            GenericParameters genericParameters = GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST_URI)
                    .procedure(PROCEDURE)
                    .build();

            // TODO config to send External events
            NewRelic.getAgent().getInsights().recordCustomEvent(EXTERNAL_CALL_EVENT, getExternalParametersMap(genericParameters));

            NewRelic.getAgent().getTracedMethod().reportAsExternal(genericParameters);
        }
    }

    public static void processResponse(URI requestUri, Response response) {
        if (response != null) {
            HttpParameters httpParameters = HttpParameters
                    .library(LIBRARY)
                    .uri(requestUri)
                    .procedure(PROCEDURE)
                    .inboundHeaders(new InboundWrapper(response))
                    .build();

            // TODO config to send External events
            NewRelic.getAgent().getInsights().recordCustomEvent(EXTERNAL_CALL_EVENT, getExternalParametersMap(httpParameters));

            TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
            // TODO config to send unscoped external metrics
            if (tracedMethod instanceof NoOpTracedMethod) {
                // this would create unscoped external metrics for the externals page but they wouldn't have correct response times
//                ExternalMetrics.recordUnscopedExternalMetrics(requestUri);
            } else {
                tracedMethod.reportAsExternal(httpParameters);
            }
        }
    }

}
