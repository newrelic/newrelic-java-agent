/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.JmxType;

public enum JtaJmxMetricGenerator {

    COUNT {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_COUNT, JmxAction.SUM_ALL,
                    JmxType.MONOTONICALLY_INCREASING);
        }
    },

    COMMIT {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_COMMIT, JmxType.MONOTONICALLY_INCREASING);
        }
    },

    ROLLBACK {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_ROLLBACK, JmxType.MONOTONICALLY_INCREASING);
        }
    },

    TIMEOUT {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_TIMEOUT, JmxType.MONOTONICALLY_INCREASING);
        }
    },

    ABANDONDED {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_ABANDONED, JmxType.MONOTONICALLY_INCREASING);
        }
    };

    public abstract JmxMetric createMetric(String... attributeName);
}
