/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk2.services.s3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.S3Response;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class S3MetricUtil {

    private static final String SERVICE = "S3";

    public static void reportExternalMetrics(Segment segment, String uri, S3Response s3Response, String operationName) {
        try {

            Integer statusCode = null;
            String statusText = null;
            if (s3Response != null) {
                statusCode = s3Response.sdkHttpResponse().statusCode();
                statusText = s3Response.sdkHttpResponse().statusText().orElse(null);
            }
            HttpParameters httpParameters = HttpParameters.library(SERVICE)
                    .uri(new URI(uri))
                    .procedure(operationName)
                    .noInboundHeaders()
                    .status(statusCode, statusText)
                    .build();
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

    public static void reportExternalMetrics(TracedMethod tracedMethod, String uri, S3Response s3Response, String operationName) {
        try {
            Integer statusCode = null;
            String statusText = null;
            if (s3Response != null) {
                statusCode = s3Response.sdkHttpResponse().statusCode();
                statusText = s3Response.sdkHttpResponse().statusText().orElse(null);
            }
            HttpParameters httpParameters = HttpParameters.library(SERVICE)
                    .uri(new URI(uri))
                    .procedure(operationName)
                    .noInboundHeaders()
                    .status(statusCode, statusText)
                    .build();
            tracedMethod.reportAsExternal(httpParameters);
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
    }

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
