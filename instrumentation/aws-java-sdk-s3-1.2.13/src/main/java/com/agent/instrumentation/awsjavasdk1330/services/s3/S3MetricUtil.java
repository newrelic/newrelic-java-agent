/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1330.services.s3;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.external.ExternalMetrics;
import com.newrelic.api.agent.ExternalParameters;

/**
 * This uses {@link ExternalMetrics} to create external metrics for all S3 calls in
 * {@link com.amazonaws.services.s3.AmazonS3_Instrumentation}.
 *
 * <p>
 * It should be updated to use {@link TracedMethod#reportAsExternal(ExternalParameters)} at some point.
 */
public abstract class S3MetricUtil {

    private static final String HOST = "amazon";
    private static final String SERVICE = "S3";
    private static final String URI = "";

    public static void metrics(Transaction transaction, TracedMethod tracedMethod, String operationName) {
        ExternalMetrics.makeExternalComponentTrace(transaction, tracedMethod, HOST, SERVICE, false, URI, operationName);
    }

}
