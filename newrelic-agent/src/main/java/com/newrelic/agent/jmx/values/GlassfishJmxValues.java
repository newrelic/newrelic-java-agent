/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.values;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxInvokeValue;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

import java.util.ArrayList;
import java.util.List;

public class GlassfishJmxValues extends JmxFrameworkValues {

    private static final int METRIC_COUNT = 3;
    private static final int INVOKE_COUNT = 1;

    private static final List<BaseJmxValue> METRICS = new ArrayList<>(METRIC_COUNT);
    private static final List<BaseJmxInvokeValue> INVOKERS = new ArrayList<>(INVOKE_COUNT);

    public static final String PREFIX = "amx";

    private static final JmxMetric CURRENT_IDLE_COUNT = JmxMetric.create(new String[] { "currentthreadcount.count",
            "currentthreadsbusy.count" }, MetricNames.JMX_THREAD_POOL_IDLE, JmxAction.SUBTRACT_ALL_FROM_FIRST,
            JmxType.SIMPLE);

    static {
        /*
         * Provides thread pool information. Glassfish returns CompositeDataSupport objects which provides the name,
         * description, lastSampleTime, startTime, unit, and count. We want the count which is the value in each
         * instance.
         */
        METRICS.add(new BaseJmxValue("amx:type=thread-pool-mon,pp=*,name=*", MetricNames.JMX_THREAD_POOL + "{name}/",
                new JmxMetric[] {
                        ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("currentthreadsbusy.count"),
                        ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("maxthreads.count"),
                        CURRENT_IDLE_COUNT }));

        /*
         * Provides session information. Glassfish returns CompositeDataSupport objects which provides the name,
         * description, lastSampleTime, startTime, unit, and count. We want the count which is the value in each
         * instance.
         */
        METRICS.add(new BaseJmxValue("amx:type=session-mon,pp=*,name=*", MetricNames.JMX_SESSION + "{name}/",
                new JmxMetric[] {
                        ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT.createMetric("activesessionscurrent.current"),
                        ServerJmxMetricGenerator.SESSION_EXPIRED_COUNT.createMetric("expiredsessionstotal.count"),
                        ServerJmxMetricGenerator.SESSION_REJECTED_COUNT.createMetric("rejectedsessionstotal.count") }));

        /*
         * Provides transaction information. The active count is the number of currently active transactions. The
         * committed count is the number of transactions which have been committed. rolledback count is the number of
         * transactions that have been rolled back.
         */
        METRICS.add(new BaseJmxValue("amx:type=transaction-service-mon,pp=*,name=*", MetricNames.JMX_TRANSACITON,

        new JmxMetric[] { ServerJmxMetricGenerator.TRANS_ACTIVE_COUNT.createMetric("activecount.count"),
                ServerJmxMetricGenerator.TRANS_COMMITED_COUNT.createMetric("committedcount.count"),
                ServerJmxMetricGenerator.TRANS_ROLLED_BACK_COUNT.createMetric("rolledbackcount.count") }));

        /* This must be called to get Glassfish metrics. */
        INVOKERS.add(new BaseJmxInvokeValue("amx-support:type=boot-amx", "bootAMX", new Object[0], new String[0]));
    }

    public GlassfishJmxValues() {
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

    @Override
    public List<BaseJmxInvokeValue> getJmxInvokers() {
        return INVOKERS;
    }

}
