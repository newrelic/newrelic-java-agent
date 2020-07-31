/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.JmxType;

public enum ServerJmxMetricGenerator {

    MAX_THREAD_POOL_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_THREAD_POOL_MAX, JmxType.SIMPLE);
        }
    },
    ACTIVE_THREAD_POOL_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_THREAD_POOL_ACTIVE, JmxType.SIMPLE);
        }
    },
    IDLE_THREAD_POOL_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_THREAD_POOL_IDLE, JmxType.SIMPLE);
        }
    },
    STANDBY_THREAD_POOL_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_THREAD_POOL_STANDBY, JmxType.SIMPLE);
        }
    },

    SESSION_AVG_ALIVE_TIME {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_SESSION_ALIVE_TIME, JmxType.SIMPLE);
        }
    },
    SESSION_ACTIVE_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_SESSION_ACTIVE, JmxType.SIMPLE);
        }
    },
    SESSION_REJECTED_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_SESSION_REJECTED, JmxType.MONOTONICALLY_INCREASING);
        }
    },
    SESSION_EXPIRED_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_SESSION_EXPIRED, JmxType.MONOTONICALLY_INCREASING);
        }
    },

    TRANS_ACTIVE_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_TRANS_ACTIVE, JmxType.SIMPLE);
        }
    },
    TRANS_NESTED_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_TRANS_NESTED, JmxType.MONOTONICALLY_INCREASING);
        }
    },
    TRANS_COMMITED_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_TRANS_COMMITTED, JmxType.MONOTONICALLY_INCREASING);
        }
    },
    TRANS_ROLLED_BACK_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_TRANS_ROLLED_BACK, JmxType.MONOTONICALLY_INCREASING);
        }
    },
    TRANS_TOP_LEVEL_COUNT {
        @Override
        public JmxMetric createMetric(String pAttributeName) {
            return JmxMetric.create(pAttributeName, MetricNames.JMX_TRANS_TOP_LEVEL, JmxType.MONOTONICALLY_INCREASING);
        }
    };

    public abstract JmxMetric createMetric(String attributeName);
}
