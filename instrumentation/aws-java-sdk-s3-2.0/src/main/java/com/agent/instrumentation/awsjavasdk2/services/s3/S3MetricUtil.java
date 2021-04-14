/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk2.services.s3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.S3Response;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class S3MetricUtil {

    private static final String SERVICE = "S3";

    public static void reportExternalMetrics(Segment segment, String uri, String operationName, Integer statusCode) {
        try {
            HttpParameters httpParameters = getHttpParameters(uri, operationName, statusCode, null);
            segment.reportAsExternal(httpParameters);
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
    }

    public static void reportExternalMetrics(Segment segment, String uri, String operationName, S3Response s3Response) {
        try {
            HttpParameters httpParameters = getHttpParameters(uri, operationName, s3Response);
            segment.reportAsExternal(httpParameters);
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
    }

    public static void reportExternalMetrics(TracedMethod tracedMethod, String uri, String operationName, Integer statusCode) {
        try {
            HttpParameters httpParameters = getHttpParameters(uri, operationName, statusCode, null);
            tracedMethod.reportAsExternal(httpParameters);
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
    }

    public static void reportExternalMetrics(TracedMethod tracedMethod, String uri, String operationName, S3Response s3Response) {
        try {
            HttpParameters httpParameters = getHttpParameters(uri, operationName, s3Response);
            tracedMethod.reportAsExternal(httpParameters);
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
    }

    private static HttpParameters getHttpParameters(String uri, String operationName, S3Response s3Responseesponse) throws URISyntaxException {
        SdkHttpResponse httpResponse = s3Responseesponse.sdkHttpResponse();
        int statusCode = httpResponse.statusCode();
        String statusText = httpResponse.statusText().orElse(null);
        return getHttpParameters(uri, operationName, statusCode, statusText);
    }

    private static HttpParameters getHttpParameters(String uri, String operationName, Integer statusCode, String statusText) throws URISyntaxException {
        return HttpParameters.library(SERVICE)
                .uri(new URI(uri))
                .procedure(operationName)
                .noInboundHeaders()
                .status(statusCode, statusText)
                .build();
    }
}
