/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.tracers.Tracer;

class ExternalRequestImpl extends RequestImpl implements ExternalRequest {

    private static Pattern EXTERNAL_METRIC = Pattern.compile("External/([^/]+)/(.+)");
    private static Pattern EXTERNAL_SEGMENT = Pattern.compile("External/([^/]+)/([^/]+)(/(.+))?");

    private static Pattern EXTERNAL_TX_METRIC = Pattern.compile("ExternalTransaction/([^/]+)/(.+)");
    private static Pattern EXTERNAL_TX_SEGMENT = Pattern.compile("ExternalTransaction/([^/]+)/(.+)");

    private String library;
    private String operation;
    private String segmentName;
    private String catTransactionGuid;

    private ExternalRequestImpl(String originalMetric, String segmentName, String host, String lib, String operation,
            String catTransactionGuid) {
        super(originalMetric, host);
        this.library = lib;
        this.segmentName = segmentName;
        this.operation = operation;
        this.catTransactionGuid = catTransactionGuid;
    }

    public static ExternalRequestImpl checkAndMakeExternal(Tracer transactionSegment) {

        String transactionSegmentName = transactionSegment.getTransactionSegmentName();
        String metricName = transactionSegment.getMetricName();
        Matcher segMatcher = EXTERNAL_SEGMENT.matcher(transactionSegmentName);
        Matcher metricMatcher = EXTERNAL_METRIC.matcher(metricName);
        if (segMatcher.matches() && (segMatcher.groupCount() == 2 || segMatcher.groupCount() == 4)
                && metricMatcher.matches() && metricMatcher.groupCount() == 2) {
            String host = segMatcher.group(1);
            String lib = segMatcher.group(2);
            String op = null;
            if (segMatcher.groupCount() == 4) {
                op = segMatcher.group(4);
            }
            return new ExternalRequestImpl(metricName, transactionSegmentName, host, lib, op,
                    (String) transactionSegment.getAgentAttribute("transaction_guid"));
        } else {
            segMatcher = EXTERNAL_TX_METRIC.matcher(transactionSegmentName);
            metricMatcher = EXTERNAL_TX_SEGMENT.matcher(metricName);
            if (segMatcher.matches() && segMatcher.groupCount() == 2 && metricMatcher.matches()
                    && metricMatcher.groupCount() == 2) {
                String host = segMatcher.group(1);
                String lib = null;
                String op = null;
                return new ExternalRequestImpl(metricName, transactionSegmentName, host, lib, op,
                        (String) transactionSegment.getAgentAttribute("transaction_guid"));
            }
        }
        return null;
    }

    @Override
    protected boolean wasMerged(RequestImpl potential) {
        /*
         * The metric name could be the same but the segment name might be different if the operations are different.
         */
        if ((segmentName != null) && (potential instanceof ExternalRequestImpl)
                && segmentName.equals(((ExternalRequestImpl) potential).segmentName)) {
            return super.wasMerged(potential);
        }
        return false;
    }

    @Override
    public String getLibrary() {
        return library;
    }

    @Override
    public String getOperation() {
        return this.operation;
    }

    String getSegmentName() {
        return segmentName;
    }

    @Override
    public String getTransactionGuild() {
        return catTransactionGuid;
    }

}
