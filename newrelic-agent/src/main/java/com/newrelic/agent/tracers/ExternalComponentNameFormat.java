/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;

public class ExternalComponentNameFormat implements MetricNameFormat {

    private String metricName;
    private String transactionSegmentName;
    private final String[] operations;
    private final boolean includeOperationInMetric;
    private final String host;
    private final String library;
    private final String transactionSegmentUri;

    public ExternalComponentNameFormat(String host, String library, boolean includeOperationInMetric,
            String pTransactionSegmentUri, String[] operations) {
        this.host = host;
        this.library = library;
        this.operations = operations;
        this.includeOperationInMetric = includeOperationInMetric;
        transactionSegmentUri = pTransactionSegmentUri;

        setMetricName();
    }

    public ExternalComponentNameFormat cloneWithNewHost(String hostName) {
        return new ExternalComponentNameFormat(hostName, this.library, this.includeOperationInMetric,
                this.transactionSegmentUri, this.operations);
    }

    private void setMetricName() {
        metricName = Strings.join('/', MetricNames.EXTERNAL_PATH, host, library);
        if (includeOperationInMetric) {
            metricName += fixOperations(operations);
            transactionSegmentName = metricName;
        }
    }

    @Override
    public String getMetricName() {
        return metricName;
    }

    @Override
    public String getTransactionSegmentName() {
        if (transactionSegmentName == null) {
            transactionSegmentName = metricName + fixOperations(operations);
        }
        return transactionSegmentName;
    }

    private String fixOperations(String[] operations) {
        StringBuilder builder = new StringBuilder();
        for (String operation : operations) {
            if (operation.startsWith("/")) {
                builder.append(operation);
            } else {
                builder.append('/').append(operation);
            }
        }
        return builder.toString();
    }

    @Override
    public String getTransactionSegmentUri() {
        return transactionSegmentUri;
    }

    public static MetricNameFormat create(String host, String library, boolean includeOperationInMetric, String uri,
            String... operations) {
        return new ExternalComponentNameFormat(host, library, includeOperationInMetric, uri, operations);
    }

}
