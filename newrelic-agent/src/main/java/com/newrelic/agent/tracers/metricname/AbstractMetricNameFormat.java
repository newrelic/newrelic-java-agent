/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.metricname;

public abstract class AbstractMetricNameFormat implements MetricNameFormat {

    @Override
    public String getTransactionSegmentName() {
        return getMetricName();
    }
    
    public String getTransactionSegmentUri() {
        return "";
    }

}
