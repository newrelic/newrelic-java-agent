/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.deps.com.google.common.collect.LinkedListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.ListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.Maps;
import com.newrelic.agent.introspec.DataStoreRequest;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.stats.TransactionStats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class IntrospectTxData {

    private int count;
    private final Set<String> txNames = new HashSet<>();
    private final Set<String> unfinishedTransactionGuids = new HashSet<>();
    private final ListMultimap<String, com.newrelic.agent.introspec.TransactionEvent> txEvents = LinkedListMultimap.create();
    private final Map<String, ExternalsForTransaction> externalRequests = new ConcurrentHashMap<>();
    private final Map<String, DatastoreForTransaction> datastoresByTx = new ConcurrentHashMap<>();

    public IntrospectTxData() {
    }

    public int getTxCount() {
        synchronized (this) {
            return count;
        }
    }

    public int getUnfinishedTxCount() {
        synchronized (this) {
            return unfinishedTransactionGuids.size();
        }
    }

    public void addStartedTransaction(Transaction transaction) {
        unfinishedTransactionGuids.add(transaction.getGuid());
    }

    public void addFinishedTransaction(TransactionData data, TransactionStats stats) {
        try {
            synchronized (this) {
                String txName = data.getPriorityTransactionName().getName();
                txNames.add(txName);

                if (ExternalsForTransaction.hasExternals(stats)) {
                    ExternalsForTransaction txext = externalRequests.get(txName);
                    if (txext == null) {
                        txext = new ExternalsForTransaction();
                        externalRequests.put(txName, txext);
                    }
                    Collection<com.newrelic.agent.tracers.Tracer> tracers = new ArrayList<>();
                    tracers.add(data.getRootTracer());
                    tracers.addAll(data.getTracers());
                    txext.addExternals(data.isWebTransaction(), stats, tracers);
                }

                if (DatastoreForTransaction.hasDatastores(stats)) {
                    DatastoreForTransaction txdatastores = datastoresByTx.get(txName);
                    if (txdatastores == null) {
                        txdatastores = new DatastoreForTransaction();
                        datastoresByTx.put(txName, txdatastores);
                    }
                    txdatastores.addDatastore(data.isWebTransaction(), stats);
                }

                TransactionEvent currentEvent = ServiceFactory.getTransactionEventsService().createEvent(data, stats, txName);
                txEvents.put(txName, new com.newrelic.agent.introspec.internal.TransactionEventImpl(currentEvent));
                count++;

                unfinishedTransactionGuids.remove(data.getGuid());
            }
        } catch (Throwable e) {
            // mistake in the test framework
            throw new RuntimeException(e);
        }
    }

    public Collection<DataStoreRequest> getDatastores(String txName) {
        DatastoreForTransaction datastores = datastoresByTx.get(txName);
        if (datastores == null) {
            return null;
        } else {
            return Collections.unmodifiableCollection(datastores.getDatastores());
        }
    }

    public Collection<ExternalRequest> getExternals(String txName) {
        ExternalsForTransaction exts = externalRequests.get(txName);
        if (exts == null) {
            return null;
        } else {
            return Collections.unmodifiableCollection(exts.getExternals());
        }
    }

    public Collection<com.newrelic.agent.introspec.TransactionEvent> getTransactionEvents(String txName) {
        return txEvents.get(txName);
    }

    public Collection<String> getTransactionNames() {
        synchronized (this) {
            return Collections.unmodifiableCollection(txNames);
        }
    }

    public void clear() {
        synchronized (this) {
            txNames.clear();
            txEvents.clear();
            datastoresByTx.clear();
            externalRequests.clear();
            count = 0;
            unfinishedTransactionGuids.clear();
        }
    }

}
