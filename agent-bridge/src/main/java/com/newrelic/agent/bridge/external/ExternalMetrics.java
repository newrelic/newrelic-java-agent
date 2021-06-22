/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;

import java.net.URI;
import java.net.URISyntaxException;
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
        // transaction segment name always contains operations; metric name may or may not
        String operationsPath = fixOperations(operations);

        if (operationsPath == null) {
            String metricName = MessageFormat.format(METRIC_NAME, host, library);
            method.setMetricNameFormatInfo(metricName, metricName, uri);
        } else {
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

        // create a single roll up metric of all external calls
        method.addExclusiveRollupMetricName(ALL);

        // create a roll up metric for either all external calls from web transactions, or all external calls
        // from other (background) transactions
        if (isWebTransaction) {
            method.addExclusiveRollupMetricName(ALL_WEB);
        } else {
            method.addExclusiveRollupMetricName(ALL_OTHER);
        }

        // create a roll up of external calls by host
        method.addExclusiveRollupMetricName(MessageFormat.format(ALL_HOST, hostName));
    }

    // TODO what are the metric values??? We don't have access to any traced response time
    // TODO some check if this option is enabled
    // TODO NewRelic.recordMetric vs NewRelic.recordResponseTimeMetric, which makes sense?
    public static void recordUnscopedExternalMetrics(URI uri) {
//        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_REPORT_AS_EXTERNAL);
        URI sanitizedURI = sanitizeURI(uri);
        String host = (sanitizedURI != null) ? sanitizedURI.getHost() : UNKNOWN_HOST;
        NewRelic.recordResponseTimeMetric(ALL, 1);
        // TODO some kind of check for web or other?
        NewRelic.recordResponseTimeMetric(ALL_OTHER, 1);
        NewRelic.recordResponseTimeMetric(MessageFormat.format(ALL_HOST, host), 1);
//        setMetricNameFormat - External/example.com/CommonsHttp/execute
//        DefaultTracer.recordMetrics is where the bulk of metric generation occurs
//        new ResponseTimeStatsImpl();
//        stats.recordResponseTimeInNanos(getExclusiveDuration(), getExclusiveDuration());
    }

    /**
     * Reconstruct a URI, stripping out query parameters, user info, and fragment.
     *
     * @param uri uri to sanitize
     * @return reconstructed URI without userInfo, query parameters, or fragment.
     */
    public static URI sanitizeURI(URI uri) {
        try {
            if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
//                Agent.LOG.log(Level.FINE, "Invalid URI. URI parameter passed should include a valid scheme and host");
                return null;
            }

            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null);
        } catch (URISyntaxException e) {
            return null;
        }
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