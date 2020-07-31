/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.dispatchers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.stats.ApdexStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.transaction.OtherTransactionNamer;
import com.newrelic.agent.transaction.TransactionNamer;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class OtherDispatcher extends DefaultDispatcher {

    private final MetricNameFormat uri;

    public OtherDispatcher(Transaction transaction, MetricNameFormat uri) {
        super(transaction);
        this.uri = uri;
    }

    @Override
    public void setTransactionName() {
        TransactionNamer tn = OtherTransactionNamer.create(getTransaction(), getUri());
        tn.setTransactionName();
    }

    @Override
    public String getUri() {
        return uri.getMetricName();
    }

    @Override
    public TransactionTracerConfig getTransactionTracerConfig() {
        return getTransaction().getAgentConfig().getBackgroundTransactionTracerConfig();
    }

    @Override
    public void transactionFinished(String transactionName, TransactionStats stats) {
        // represents the logical front of the task, but it is not an actual component, so it has no exclusive time
        // this should always equal transactionTimer.getTransactionDuration().
        stats.getUnscopedStats().getOrCreateResponseTimeStats(transactionName).recordResponseTime(
                getTransaction().getTransactionTimer().getResponseTimeInNanos(), 0, TimeUnit.NANOSECONDS);

        // the total time
        if (hasTransactionName(transactionName, MetricNames.OTHER_TRANSACTION)) {
            String totalTimeMetric = getTransName(transactionName, MetricNames.OTHER_TRANSACTION,
                    MetricNames.TOTAL_TIME);
            stats.getUnscopedStats().getOrCreateResponseTimeStats(totalTimeMetric).recordResponseTime(
                    getTransaction().getTransactionTimer().getTotalSumTimeInNanos(), 0, TimeUnit.NANOSECONDS);
        }

        stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.OTHER_TRANSACTION_ALL).recordResponseTime(
                getTransaction().getTransactionTimer().getResponseTimeInNanos(),
                getTransaction().getTransactionTimer().getResponseTimeInNanos(), TimeUnit.NANOSECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.OTHER_TRANSACTION_TOTAL_TIME).recordResponseTime(
                getTransaction().getTransactionTimer().getTotalSumTimeInNanos(),
                getTransaction().getTransactionTimer().getTotalSumTimeInNanos(), TimeUnit.NANOSECONDS);

        recordApdexMetrics(transactionName, stats);

        Object cpuTime = getTransaction().getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
        if (cpuTime != null && cpuTime instanceof Long) {
            long val = (Long) cpuTime;
            String cpuMetricName = MetricNames.CPU_PREFIX + transactionName;
            stats.getUnscopedStats().getOrCreateResponseTimeStats(cpuMetricName).recordResponseTimeInNanos(val);
            stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.CPU_OTHER).recordResponseTimeInNanos(val);
        }
    }

    private void recordApdexMetrics(String transactionName, TransactionStats stats) {
        if (transactionName == null || transactionName.length() == 0) {
            return;
        }
        if (!getTransaction().getAgentConfig().isApdexTSet(transactionName)) {
            return;
        }
        if (isIgnoreApdex()) {
            Agent.LOG.log(Level.FINE, "Ignoring transaction for Apdex {0}", transactionName);
            return;
        }
        String apdexMetricName = getApdexMetricName(transactionName, MetricNames.OTHER_TRANSACTION,
                MetricNames.APDEX_OTHER_TRANSACTION);
        if (apdexMetricName == null || apdexMetricName.length() == 0) {
            return;
        }
        long apdexT = getTransaction().getAgentConfig().getApdexTInMillis(transactionName);

        ApdexStats apdexStats = stats.getUnscopedStats().getApdexStats(apdexMetricName);
        ApdexStats overallApdexStats = stats.getUnscopedStats().getApdexStats(MetricNames.APDEX_OTHER);

        if (isApdexFrustrating()) {
            apdexStats.recordApdexFrustrated();
            overallApdexStats.recordApdexFrustrated();
        } else {
            long responseTimeInMillis = getTransaction().getTransactionTimer().getResponseTimeInMilliseconds();
            apdexStats.recordApdexResponseTime(responseTimeInMillis, apdexT);
            overallApdexStats.recordApdexResponseTime(responseTimeInMillis, apdexT);
        }
    }

    public boolean isApdexFrustrating() {
        return getTransaction().isErrorReportableAndNotIgnored() && getTransaction().isErrorNotExpected();
    }

    @Override
    public boolean isWebTransaction() {
        return false;
    }

    @Override
    public String getCookieValue(String name) {
        return null;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Request getRequest() {
        return null;
    }

    @Override
    public void setRequest(Request request) {
    }

    @Override
    public Response getResponse() {
        return null;
    }

    @Override
    public void setResponse(Response response) {
    }

    @Override
    public void transactionActivityWithResponseFinished() {
        // no op - not used for background transactions
    }

}
