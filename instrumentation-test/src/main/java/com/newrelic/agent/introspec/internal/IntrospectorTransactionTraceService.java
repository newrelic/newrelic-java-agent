/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.deps.com.google.common.collect.ArrayListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.ListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.Multimaps;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class IntrospectorTransactionTraceService extends TransactionTraceService {

    private ListMultimap<String, TransactionTrace> traces = Multimaps.synchronizedListMultimap(ArrayListMultimap.<String, TransactionTrace>create());

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        com.newrelic.agent.trace.TransactionTrace trace = com.newrelic.agent.trace.TransactionTrace.getTransactionTrace(transactionData);
        String txName = transactionData.getPriorityTransactionName().getName();
        try {
            traces.put(txName, TransactionTraceImpl.createTransactionTrace(trace));
        } catch (Exception e) {
            // just ignore it for now
        }
    }

    public Collection<TransactionTrace> getTracesForTransaction(String txName) {
        List<TransactionTrace> txTraces = traces.get(txName);
        if (txTraces == null) {
            return null;
        } else {
            return Collections.unmodifiableCollection(txTraces);
        }
    }

    public void clear() {
        traces.clear();
    }

    @Override
    protected void doStart() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @Override
    protected void doStop() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

}
