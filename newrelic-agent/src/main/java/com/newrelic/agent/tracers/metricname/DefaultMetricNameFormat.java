/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.metricname;

import java.text.MessageFormat;

import com.newrelic.agent.tracers.ClassMethodSignature;

public class DefaultMetricNameFormat extends AbstractMetricNameFormat {

    private final String metricName;

    public DefaultMetricNameFormat(ClassMethodSignature sig, Object object, String pattern) {
        metricName = MessageFormat.format(pattern, object.getClass().getName(), sig.getMethodName());
    }

    @Override
    public String getMetricName() {
        return metricName;
    }

}
