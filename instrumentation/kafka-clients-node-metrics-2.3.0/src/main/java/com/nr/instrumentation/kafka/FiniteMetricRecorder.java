/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;

class FiniteMetricRecorder {
    public void incrementCounter(final String metric, final int value) {
        NewRelic.incrementCounter(metric, value);
    }

    public boolean tryRecordMetric(final String metric, final float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return false;
        }

        NewRelic.recordMetric(metric, value);
        return true;
    }

    public void recordMetric(final String metric, final float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return;
        }

        NewRelic.recordMetric(metric, value);
    }
}
