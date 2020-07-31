/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.deps.com.google.common.collect.LinkedListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.ListMultimap;
import com.newrelic.agent.deps.com.google.common.collect.Multimaps;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.errors.ThrowableError;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.introspec.Error;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.stats.TransactionStats;

import java.net.HttpURLConnection;
import java.util.*;

class IntrospectorErrorService extends ErrorServiceImpl {

    private List<Error> errorsOutsideTransactions = Collections.synchronizedList(new LinkedList<Error>());
    private ListMultimap<String, Error> errors = Multimaps.synchronizedListMultimap(LinkedListMultimap.<String, Error>create());
    private List<com.newrelic.agent.introspec.ErrorEvent> errorEventsOutsideTransactions =
            Collections.synchronizedList(new LinkedList<com.newrelic.agent.introspec.ErrorEvent>());
    private ListMultimap<String, com.newrelic.agent.introspec.ErrorEvent> errorEvents =
            Multimaps.synchronizedListMultimap(LinkedListMultimap.<String, com.newrelic.agent.introspec.ErrorEvent>create());

    public IntrospectorErrorService() {
        super("TestApp");
    }

    @Override
    protected void reportError(TracedError error, TransactionData transactionData,
            TransactionStats transactionStats) {
        if (error == null) {
            return;
        }
        if (error instanceof ThrowableError && getErrorAnalyzer().isIgnoredError(HttpURLConnection.HTTP_OK, ((ThrowableError) error).getThrowable())) {
            return;
        }

        ErrorEvent event = ErrorServiceImpl.createErrorEvent("TestApp", error, transactionData, transactionStats);
        if (transactionData == null) {
            errorsOutsideTransactions.add(new ErrorImpl(error));
            errorEventsOutsideTransactions.add(new ErrorEventImpl(event));
        } else {
            String txName = transactionData.getPriorityTransactionName().getName();
            errors.put(txName, new ErrorImpl(error));
            errorEvents.put(txName, new ErrorEventImpl(event));
        }
    }

    public void clear() {
        errorsOutsideTransactions.clear();
        errors.clear();
        errorEventsOutsideTransactions.clear();
        errorEvents.clear();
    }

    public Collection<com.newrelic.agent.introspec.ErrorEvent> getErrorEvents() {
        List<com.newrelic.agent.introspec.ErrorEvent> allEvents = new ArrayList<>();
        allEvents.addAll(errorEventsOutsideTransactions);
        allEvents.addAll(errorEvents.values());
        return Collections.unmodifiableCollection(allEvents);
    }

    public Collection<com.newrelic.agent.introspec.ErrorEvent> getErrorEventsForTransaction(String txName) {
        List<com.newrelic.agent.introspec.ErrorEvent> output = errorEvents.get(txName);
        if (output == null) {
            return null;
        } else {
            return Collections.unmodifiableCollection(output);
        }
    }

    public Collection<Error> getAllErrors() {
        List<Error> allErrors = new ArrayList<>();
        allErrors.addAll(errorsOutsideTransactions);
        allErrors.addAll(errors.values());
        return Collections.unmodifiableCollection(allErrors);
    }

    public Collection<Error> getErrors(String transactionName) {
        List<Error> output = errors.get(transactionName);
        if (output == null) {
            return null;
        } else {
            return Collections.unmodifiableCollection(output);
        }
    }

}
