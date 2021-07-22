/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.*;
import com.newrelic.api.agent.weaver.Weaver;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class GraphQLMetricUtil {
    private static final String SERVICE = "GraphQL";

    public static void reportExternalMetrics(Segment segment, String uri, String operationName) {
        try {
            HttpParameters httpParameters =
                    HttpParameters.library(SERVICE)
                            .uri(new URI(uri))
                            .procedure(operationName)
                            .noInboundHeaders().build();
            segment.reportAsExternal(httpParameters);
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
    }

    public static void reportExternalMetrics(TracedMethod tracedMethod, String uri, String operationName) {
        try {
            HttpParameters httpParameters = HttpParameters.library(SERVICE).uri(new URI(uri)).procedure(operationName).noInboundHeaders().build();
            tracedMethod.reportAsExternal(httpParameters);
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }

    }

    public static void metrics(TracedMethod tracedMethod, String operationName) {
        try {
            GenericParameters params = GenericParameters
                    .library("GraphQL")
                    .uri(URI.create("/query"))
                    .procedure("post")
                    .build();

            tracedMethod.reportAsExternal(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
