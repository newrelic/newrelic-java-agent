/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.values;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * This works for 7.1 AS and 6.0 EAP.
 */
public class Jboss7UpJmxValues extends JmxFrameworkValues {

    private static final int METRIC_COUNT = 1;

    private static final List<BaseJmxValue> METRICS = new ArrayList<>(METRIC_COUNT);

    public static final String PREFIX = "jboss.as";

    static {

        /*
         * Provides information on transactions. The number of nested transactions is the number of nested (sub)
         * transactions. The numberOfTransactions is the number of transactions (top-level) and nested which have been
         * created. The numberOfInflightTransactions is the number of transactions which have begun but have not yet
         * terminated. The numberOfApplicationRollbacks is the number of transactions that have been rolled back by
         * application request (this includes timed out transactions). The numberOfResourceRollbacks is the number of
         * transactions which have been rolled back due to resource (participant) failure.
         * 
         * This is present in 6.0EAP and 7.1 AS.
         */
        METRICS.add(new BaseJmxValue("jboss.as:subsystem=transactions", MetricNames.JMX_TRANSACITON, new JmxMetric[] {
                ServerJmxMetricGenerator.TRANS_ROLLED_BACK_COUNT.createMetric("numberOfAbortedTransactions"),
                ServerJmxMetricGenerator.TRANS_COMMITED_COUNT.createMetric("numberOfCommittedTransactions"),
                ServerJmxMetricGenerator.TRANS_ACTIVE_COUNT.createMetric("numberOfInflightTransactions"),
                ServerJmxMetricGenerator.TRANS_NESTED_COUNT.createMetric("numberOfNestedTransactions"),
                JmxMetric.create(new String[] { "numberOfTransactions", "numberOfNestedTransactions" },
                        MetricNames.JMX_TRANS_TOP_LEVEL, JmxAction.SUBTRACT_ALL_FROM_FIRST,
                        JmxType.MONOTONICALLY_INCREASING) }));
    }

    public Jboss7UpJmxValues() {
        super();
    }

    @Override
    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

}
