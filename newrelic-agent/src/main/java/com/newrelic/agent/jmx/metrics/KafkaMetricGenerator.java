/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.jmx.JmxType;

public enum KafkaMetricGenerator {

    COUNT_MONOTONIC {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, "", JmxType.MONOTONICALLY_INCREASING);
        }
    },
    VALUE_SIMPLE {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, "", JmxType.SIMPLE);
        }
    },
    QUEUE_SIZE {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, "", JmxType.SIMPLE);
        }
    },
    // this will get req mean size
    REQ_MEAN {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, "", JmxType.SIMPLE);
        }
    };

    public abstract JmxMetric createMetric(String attributeName);
}
