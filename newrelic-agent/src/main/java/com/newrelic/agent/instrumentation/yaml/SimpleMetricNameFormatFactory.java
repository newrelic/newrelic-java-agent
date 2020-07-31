/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.yaml;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public class SimpleMetricNameFormatFactory implements MetricNameFormatFactory {
    private final MetricNameFormat metricNameFormat;

    public SimpleMetricNameFormatFactory(MetricNameFormat metricNameFormat) {
        super();
        this.metricNameFormat = metricNameFormat;
    }

    @Override
    public MetricNameFormat getMetricNameFormat(ClassMethodSignature sig, Object object, Object[] args) {
        return metricNameFormat;
    }

}
