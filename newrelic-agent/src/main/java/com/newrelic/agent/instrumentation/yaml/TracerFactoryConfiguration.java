/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.yaml;

import java.util.Collections;
import java.util.Map;

import com.newrelic.agent.instrumentation.yaml.PointCutFactory.ClassMethodNameFormatDescriptor;
import com.newrelic.agent.tracers.metricname.OtherTransSimpleMetricNameFormat;

public class TracerFactoryConfiguration {
    private final boolean dispatcher;
    private final MetricNameFormatFactory metricNameFormatFactory;
    private final Map attributes;

    public TracerFactoryConfiguration(String defaultMetricPrefix, boolean pDispatcher, Object metricNameFormat,
            Map attributes) {
        this.attributes = Collections.unmodifiableMap(attributes);
        dispatcher = pDispatcher;

        if (metricNameFormat instanceof String) {
            metricNameFormatFactory = new SimpleMetricNameFormatFactory(new OtherTransSimpleMetricNameFormat(
                    metricNameFormat.toString()));
        } else if (null == metricNameFormat) {
            metricNameFormatFactory = new ClassMethodNameFormatDescriptor(defaultMetricPrefix, dispatcher);
        } else if (metricNameFormat instanceof MetricNameFormatFactory) {
            metricNameFormatFactory = (MetricNameFormatFactory) metricNameFormat;
        } else {
            throw new RuntimeException("Unsupported metric_name_format value");
        }
    }

    public Map getAttributes() {
        return attributes;
    }

    public MetricNameFormatFactory getMetricNameFormatFactory() {
        return metricNameFormatFactory;
    }

    public boolean isDispatcher() {
        return dispatcher;
    }

}
