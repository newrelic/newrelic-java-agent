/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.metricname;

import com.newrelic.agent.MetricNames;

public class OtherTransSimpleMetricNameFormat implements MetricNameFormat {

    private final String metricName;
    private final String transactionSegmentName;

    public OtherTransSimpleMetricNameFormat(String metricName) {
        this.metricName = transactionSegmentName = appendOtherTrans(metricName);
    }

    public OtherTransSimpleMetricNameFormat(String metricName, String transactionSegmentName) {
        this.metricName = appendOtherTrans(metricName);
        this.transactionSegmentName = transactionSegmentName;
    }

    private static String appendOtherTrans(String pMetricName) {
        if (pMetricName != null) {
            StringBuilder sb = new StringBuilder();
            if (!pMetricName.startsWith(MetricNames.OTHER_TRANSACTION + MetricNames.SEGMENT_DELIMITER_STRING)) {
                sb.append(MetricNames.OTHER_TRANSACTION);

                if (!pMetricName.startsWith(MetricNames.SEGMENT_DELIMITER_STRING)) {
                    sb.append(MetricNames.SEGMENT_DELIMITER_STRING);
                }
            }

            sb.append(pMetricName);
            return sb.toString();
        } else {
            return pMetricName;
        }
    }

    @Override
    public final String getMetricName() {
        return metricName;
    }

    @Override
    public String getTransactionSegmentName() {
        return transactionSegmentName;
    }

    @Override
    public String getTransactionSegmentUri() {
        return null;
    }
}
