/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1330.services.s3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weaver;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This uses {@link TracedMethod#reportAsExternal(ExternalParameters)} to create external metrics for all S3 calls in
 * {@link com.amazonaws.services.s3.AmazonS3_Instrumentation}.
 */
public abstract class S3MetricUtil {

    private static final String SERVICE = "S3";

    public static void reportExternalMetrics(TracedMethod tracedMethod, String uri, Integer statusCode, String operationName) {
        try {
            HttpParameters httpParameters = HttpParameters.library(SERVICE)
                    .uri(new URI(uri))
                    .procedure(operationName)
                    .noInboundHeaders()
                    .status(statusCode, null)
                    .build();
            tracedMethod.reportAsExternal(httpParameters);
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
    }

}
