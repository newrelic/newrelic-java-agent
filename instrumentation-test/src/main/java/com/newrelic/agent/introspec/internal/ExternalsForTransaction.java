/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.bridge.external.ExternalMetrics;
import com.newrelic.agent.deps.com.google.common.collect.HashMultimap;
import com.newrelic.agent.deps.com.google.common.collect.Multimap;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ExternalsForTransaction {

    private Map<String, ExternalRequest> metricNameToExternal = new HashMap<>();

    protected ExternalsForTransaction() {
        super();
    }

    protected Collection<ExternalRequest> getExternals() {
        return metricNameToExternal.values();
    }

    protected void addExternals(boolean isWebTx, TransactionStats stats, Collection<Tracer> tracers) {

        Collection<ExternalRequestImpl> externals = checkExternals(isWebTx, stats, tracers);
        if (externals != null && externals.size() > 0) {
            for (ExternalRequestImpl current : externals) {
                ExternalRequestImpl ext = (ExternalRequestImpl) metricNameToExternal.get(current.getMetricName());
                if (ext != null) {
                    ext.wasMerged(current);
                } else {
                    metricNameToExternal.put(current.getMetricName(), current);
                }
            }
        }
    }

    protected static boolean hasExternals(TransactionStats stats) {
        return stats.getUnscopedStats().getStatsMap().get(ExternalMetrics.ALL) != null;
    }

    protected static Collection<ExternalRequestImpl> checkExternals(boolean isWeb, TransactionStats stats,
            Collection<Tracer> tracers) {
        int totalCount = 0;
        // External/all
        // External/allWeb or External/allOther
        Multimap<String, ExternalRequestImpl> currentExternalsByHost = HashMultimap.create();
        Map<String, StatsBase> theStats = stats.getScopedStats().getStatsMap();
        String segment;
        String metric;
        for (Tracer current : tracers) {
            segment = current.getTransactionSegmentName();
            metric = current.getMetricName();
            if (metric.startsWith("External") && theStats.get(metric) != null) {
                ExternalRequestImpl impl = ExternalRequestImpl.checkAndMakeExternal(current);
                if (impl != null) {
                    currentExternalsByHost.put(impl.getHostname(), impl);
                    totalCount++;
                }
            }
        }

        return currentExternalsByHost.values();

    }
}
