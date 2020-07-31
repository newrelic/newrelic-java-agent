/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.JmxType;

public enum DataSourceJmxMetricGenerator {

    CONNECTIONS_AVAILABLE {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_AVAILABLE, JmxType.SIMPLE);
        }
    },

    CONNECTIONS_POOL_SIZE {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_POOL_SIZE, JmxType.SIMPLE);
        }
    },

    CONNECTIONS_CREATED {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_CREATED,
                    JmxType.MONOTONICALLY_INCREASING);
        }
    },

    CONNECTIONS_ACTIVE {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_ACTIVE, JmxType.SIMPLE);
        }
    },

    CONNECTIONS_MAX {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_MAX, JmxType.SIMPLE);
        }
    },

    CONNECTIONS_IDLE {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_IDLE, JmxType.SIMPLE);
        }
    },

    CONNECTIONS_LEAKED {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_LEAKED,
                    JmxType.MONOTONICALLY_INCREASING);
        }
    },

    CONNECTIONS_CACHE_SIZE {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_CACHE_SIZE, JmxType.SIMPLE);
        }
    },

    CONNECTION_REQUEST_WAITING_COUNT {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTION_WAITING_REQUEST_COUNT, JmxType.SIMPLE);
        }
    },

    CONNECTION_REQUEST_TOTAL_COUNT {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTION_TOTAL_REQUEST_COUNT,
                    JmxType.MONOTONICALLY_INCREASING);
        }
    },

    CONNECTION_REQUEST_SUCCESS {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTION_REQUEST_SUCCESS,
                    JmxType.MONOTONICALLY_INCREASING);
        }
    },

    CONNECTION_REQUEST_FAILURE {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTION_REQUEST_FAILURE,
                    JmxType.MONOTONICALLY_INCREASING);
        }
    },

    CONNECTIONS_MANAGED {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_MANAGED_COUNT, JmxType.SIMPLE);
        }
    },

    CONNECTIONS_DESTROYED {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_DESTROYED, JmxType.SIMPLE);
        }
    },

    CONNECTIONS_WAIT_TIME {
        @Override
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], MetricNames.JMX_CONNECTIONS_WAIT_TIME, JmxType.SIMPLE);
        }
    };

    public abstract JmxMetric createMetric(String... attributeName);
}
