/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;

import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;

/**
 * Utility class for sending External metrics.
 */
public class ExternalMetrics {

    public static final String METRIC_NAMESPACE = "External";
    public static final String METRIC_NAME = METRIC_NAMESPACE + "/{0}/{1}"; // External/amazon/S3
    public static final String TRANSACTION_SEGMENT_NAME = METRIC_NAME + "/{2}"; // External/amazon/S3/listBuckets
    public static final String ALL = METRIC_NAMESPACE + "/all"; // External/all
    public static final String ALL_WEB = METRIC_NAMESPACE + "/allWeb"; // External/allWeb
    public static final String ALL_OTHER = METRIC_NAMESPACE + "/allOther"; // External/allOther
    public static final String ALL_HOST = METRIC_NAMESPACE + "/{0}/all"; // External/amazon/all
    public static final String UNKNOWN_HOST = "UnknownHost";

    private static String fixOperations(String... operations) {
        StringBuilder builder = new StringBuilder();
        for (String operation : operations) {
            if (operation == null) {
                return null;
            }

            if (operation.startsWith("/")) {
                builder.append(operation);
            } else {
                builder.append('/').append(operation);
            }
        }
        return builder.substring(1);
    }

    /**
     * Sets metric name for external metrics (no rollup).
     *
     * Note that the URI may need to be stripped of sensitive data prior to this method being called.
     */
    public static void makeExternalComponentMetric(TracedMethod method, String host, String library,
            boolean includeOperationInMetric, String uri, String... operations) {

        if (operations == null || operations.length == 0) {
            String metricName = MessageFormat.format(METRIC_NAME, host, library);
            method.setMetricNameFormatInfo(metricName, metricName, uri);
        } else {
            // transaction segment name always contains operations; metric name may or may not
            String operationsPath = fixOperations(operations);
            String transactionSegmentName = MessageFormat.format(TRANSACTION_SEGMENT_NAME, host, library, operationsPath);

            String metricName = includeOperationInMetric ? transactionSegmentName
                    : MessageFormat.format(METRIC_NAME, host, library);

            method.setMetricNameFormatInfo(metricName, transactionSegmentName, uri);
        }
    }

    /**
     * Sets metric name and rollup names for external metrics.
     *
     * Note that the URI may need to be stripped of sensitive data prior to this method being called.
     */
    public static void makeExternalComponentTrace(Transaction transaction, TracedMethod method, String host,
            String library, boolean includeOperationInMetric, String uri, String... operations) {
        makeExternalComponentTrace(transaction.isWebTransaction(), method, host, library, includeOperationInMetric, uri,
                operations);
    }

    /**
     * Sets metric name and rollup names for external metrics.
     *
     * Note that the URI may need to be stripped of sensitive data prior to this method being called.
     */
    public static void makeExternalComponentTrace(boolean isWebTransaction, TracedMethod method, String host,
            String library, boolean includeOperationInMetric, String uri, String... operations) {
        String hostName = host == null ? UNKNOWN_HOST : host;

        makeExternalComponentMetric(method, hostName, library, includeOperationInMetric, uri, operations);

        if (UNKNOWN_HOST.equals(hostName)) {
            return; // NR doesn't add rollup metrics for "UnknownHost"
        }

        // create a single roll up metric of all external calls (i.e. External/all)
        method.addExclusiveRollupMetricName(ALL);

        // create a roll up metric for either all external calls from web transactions, or all external calls
        // from other (background) transactions
        if (isWebTransaction) {
            method.addExclusiveRollupMetricName(ALL_WEB); // (i.e. External/allWeb)
        } else {
            method.addExclusiveRollupMetricName(ALL_OTHER); // (i.e. External/allOther)
        }

        // create a roll up of external calls by host (i.e. External/hostName/all)
        method.addExclusiveRollupMetricName(MessageFormat.format(ALL_HOST, hostName));
    }

    /**
     * DEPRECATED - Call URISupport method directly
     */
    @Deprecated
    public static String getURI(URI url) {
        return URISupport.getURI(url);
    }

    /**
     * DEPRECATED - Call URISupport method directly
     */
    @Deprecated
    public static String getURI(URL url) {
        return URISupport.getURI(url);
    }

    /**
     * DEPRECATED - Call URISupport method directly
     */
    @Deprecated
    public static String getURI(String scheme, String host, int port, String path) {
        return URISupport.getURI(scheme, host, port, path);
    }
}