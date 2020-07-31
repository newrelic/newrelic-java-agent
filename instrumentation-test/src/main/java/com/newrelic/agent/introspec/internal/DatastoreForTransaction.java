/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.deps.com.google.common.collect.HashMultimap;
import com.newrelic.agent.deps.com.google.common.collect.Multimap;
import com.newrelic.agent.introspec.DataStoreRequest;
import com.newrelic.agent.stats.ResponseTimeStatsImpl;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.TransactionStats;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

class DatastoreForTransaction {

    // these should really be stored in the agent and not here
    private static final String ALL = "Datastore/all";
    private static final String ALL_WEB = "Datastore/allWeb";
    private static final String ALL_OTHER = "Datastore/allOther";
    private static final String STORE_ALL = "Datastore/{0}/all";
    private static final String STORE_ALL_WEB = "Datastore/{0}/allWeb";
    private static final String STORE_ALL_OTHER = "Datastore/{0}/allOther";

    private Map<String, DataStoreRequest> metricNameToDatastore = new HashMap<>();

    protected DatastoreForTransaction() {
        super();
    }

    protected Collection<DataStoreRequest> getDatastores() {
        return metricNameToDatastore.values();
    }

    protected static boolean hasDatastores(TransactionStats stats) {
        return stats.getUnscopedStats().getStatsMap().get(ALL) != null;
    }

    protected void addDatastore(boolean isWebTx, TransactionStats stats) {
        Collection<DatastoreRequestImpl> datastores = checkDatastores(isWebTx, stats);
        if (datastores != null && datastores.size() > 0) {
            for (DatastoreRequestImpl current : datastores) {
                DatastoreRequestImpl ext = (DatastoreRequestImpl) metricNameToDatastore.get(current.getMetricName());
                if (ext != null) {
                    ext.wasMerged(current);
                } else {
                    metricNameToDatastore.put(current.getMetricName(), current);
                }
            }
        }
    }

    protected static Collection<DatastoreRequestImpl> checkDatastores(boolean isWeb, TransactionStats stats) {
        int totalCount = 0;
        // Datastore/all
        // Datastore/allWeb or Datastore/allOther
        Multimap<String, DatastoreRequestImpl> currentbyDatastore = HashMultimap.create();
        for (Entry<String, StatsBase> current : stats.getScopedStats().getStatsMap().entrySet()) {
            if (current.getKey().startsWith("Datastore/")) {
                DatastoreRequestImpl impl = DatastoreRequestImpl.checkAndMakeDatastore(current.getKey());
                if (impl != null) {
                    currentbyDatastore.put(impl.getDatastore(), impl);
                    totalCount++;
                }
            }
        }

        // check that unscoped metrics are correct
        if (totalCount > 0) {
            Map<String, StatsBase> unscoped = stats.getUnscopedStats().getStatsMap();
            ResponseTimeStatsImpl all = (ResponseTimeStatsImpl) unscoped.get(ALL);
            ResponseTimeStatsImpl allWebOther = (ResponseTimeStatsImpl) (isWeb ? unscoped.get(ALL_WEB)
                    : unscoped.get(ALL_OTHER));
            if ((all != null) && (all.getCallCount() == totalCount) && (allWebOther != null)
                    && (allWebOther.getCallCount() == totalCount)) {
                // then check the host metrics
                Iterator<Entry<String, Collection<DatastoreRequestImpl>>> it = currentbyDatastore.asMap()
                        .entrySet()
                        .iterator();
                Entry<String, Collection<DatastoreRequestImpl>> current;
                while (it.hasNext()) {
                    current = it.next();
                    String prefix = MessageFormat.format(STORE_ALL, current.getKey());
                    ResponseTimeStatsImpl hostAll = (ResponseTimeStatsImpl) unscoped.get(prefix);
                    ResponseTimeStatsImpl hostWebOther = (ResponseTimeStatsImpl) (isWeb ? unscoped.get(prefix + "Web")
                            : unscoped.get(prefix + "Other"));
                    if (hostAll == null || hostWebOther == null || hostAll.getCallCount() != current.getValue().size()
                            || hostWebOther.getCallCount() != current.getValue().size()) {
                        it.remove();
                    }
                }

                return currentbyDatastore.values();
            }
        }

        return Collections.emptyList();
    }
}
