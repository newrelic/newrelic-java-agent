/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.JmxType;

public enum EjbPoolJmxMetricGenerator {

    SUCCESS {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_SUCCESSFUL_ATTEMPTS,
                    JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.MONOTONICALLY_INCREASING);
        }
    },

    THREADS_WAITING {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_THREADS_WAITING, JmxType.SIMPLE);
        }
    },

    DESTROY {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_DESTROY_BEANS, JmxType.MONOTONICALLY_INCREASING);
        }
    },

    FAILURE {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_FAILED_ATTEMPTS,
                    JmxType.MONOTONICALLY_INCREASING);
        }
    },
    AVAILABLE {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_AVAILABLE_BEANS, JmxType.SIMPLE);
        }
    },

    ACTIVE {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_ACTIVE_BEANS, JmxType.SIMPLE);
        }
    };

    public abstract JmxMetric createMetric(String... attributeName);
}
