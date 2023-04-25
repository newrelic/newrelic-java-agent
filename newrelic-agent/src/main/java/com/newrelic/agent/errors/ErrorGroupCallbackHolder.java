/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.errors;

import com.newrelic.agent.MetricNames;
import com.newrelic.api.agent.ErrorGroupCallback;
import com.newrelic.api.agent.NewRelic;

public class ErrorGroupCallbackHolder {

    private static volatile ErrorGroupCallback errorGroupCallback = null;

    public static void setErrorGroupCallback(ErrorGroupCallback newErrorGroupCallback) {
        errorGroupCallback = newErrorGroupCallback;
        if (newErrorGroupCallback != null) {
            NewRelic.getAgent().getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_ERROR_GROUPING_CALLBACK_ENABLED);
        }
    }

    public static ErrorGroupCallback getErrorGroupCallback() {
        return errorGroupCallback;
    }
}
