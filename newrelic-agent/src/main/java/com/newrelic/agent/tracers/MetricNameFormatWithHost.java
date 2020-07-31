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

public class MetricNameFormatWithHost implements MetricNameFormat {

    private final String host;
    private final String metricName;

    private MetricNameFormatWithHost(String host, String library) {
        this.host = host;
        this.metricName = Strings.join('/', MetricNames.EXTERNAL_PATH, host, library);
    }

    public String getHost() {
        return host;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }

    @Override
    public String getTransactionSegmentName() {
        return metricName;
    }

    @Override
    public String getTransactionSegmentUri() {
        return null;
    }

    public static MetricNameFormatWithHost create(String host, String library) {
        return new MetricNameFormatWithHost(host, library);
    }
}
