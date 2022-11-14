/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public abstract class AbstractExternalComponentTracer extends DefaultTracer implements IgnoreChildSocketCalls {

    private static final String UNKNOWN_HOST = "UnknownHost";
    private String host;

    public AbstractExternalComponentTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            String host, String library, String uri, String... operations) {
        this(transaction, sig, object, host, library, false, uri, operations);
    }

    public AbstractExternalComponentTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            String host, String library, boolean includeOperationInMetric, String uri, String... operations) {
        super(transaction, sig, object, ExternalComponentNameFormat.create(host, library, includeOperationInMetric,
                uri, operations));
        this.host = host;
    }

    public AbstractExternalComponentTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            String host, MetricNameFormat metricNameFormat) {
        super(transaction, sig, object, metricNameFormat);
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    @Override
    public void finish(Throwable throwable) {
        if (throwable instanceof UnknownHostException) {
            host = UNKNOWN_HOST;
            MetricNameFormat metricNameFormat = getMetricNameFormat();
            if (metricNameFormat instanceof ExternalComponentNameFormat) {
                setMetricNameFormat(((ExternalComponentNameFormat) metricNameFormat).cloneWithNewHost(UNKNOWN_HOST));
            }
        }
        super.finish(throwable);
    }

    @Override
    public void finish(int opcode, Object returnValue) {
        super.finish(opcode, returnValue);
    }

    @Override
    protected void doRecordMetrics(TransactionStats transactionStats) {
        super.doRecordMetrics(transactionStats);

        // create a single roll up metric of all external calls
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.EXTERNAL_ALL).recordResponseTime(
                getExclusiveDuration(), TimeUnit.NANOSECONDS);
        // create a roll up metric for either all external calls from web transactions, or all external calls
        // from other (background) transactions
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(
                (getTransaction().isWebTransaction() ? MetricNames.WEB_TRANSACTION_EXTERNAL_ALL
                        : MetricNames.OTHER_TRANSACTION_EXTERNAL_ALL)).recordResponseTime(getExclusiveDuration(),
                TimeUnit.NANOSECONDS);
        // create a roll up of external calls by host
        if (Agent.LOG.isFinestEnabled() && "Unknown".equals(host)) {
            Agent.LOG.log(Level.FINEST, new Exception(), "Adding /External/Unknown/all rollup metric.");
        }

        String hostRollupMetricName = Strings.join('/', MetricNames.EXTERNAL_PATH, getHost(), "all");
        transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(hostRollupMetricName).recordResponseTime(
                getExclusiveDuration(), TimeUnit.NANOSECONDS);
    }

}
