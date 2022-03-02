/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import com.newrelic.agent.model.LogEvent;

import java.util.Collection;
import java.util.Map;

/**
 * Used to test an instrumentation module. Collects the transaction data seen by the agent. This class can be used to
 * verify the data collected.
 */
public interface Introspector {

    /**
     * Throws away all data that has been collected since the last call to initialize or clear.
     */
    void clear();

    /**
     * The number of transactions that have finished.
     * 
     * @return The number of transactions that have finished since initialize or clear was called.
     */
    int getFinishedTransactionCount();

    /**
     * The number of transactions that have finished within the specified timeout. This method will wait for all
     * currently running transactions to finish, up to the timeout value
     *
     * @param timeoutInMillis the maximum amount of time to wait for transactions to finish (in milliseconds)
     * @return The number of transactions that have finished since initialize or clear was called.
     */
    int getFinishedTransactionCount(long timeoutInMillis);

    /**
     * Returns the name of the transactions which completed since the Introspector was initialized.
     * 
     * @return The transaction names.
     */
    Collection<String> getTransactionNames();

    /**
     * Returns an Map containing all of the blame metrics for the transaction with the input name.
     * 
     * @param transaction The name of the transaction.
     * @return Metrics mapped by the name of the metric.
     */
    Map<String, TracedMetricData> getMetricsForTransaction(String transaction);

    /**
     * Returns count metrics which are not apart of a transaction. The returned key is the name of the metric while the
     * {@link TracedMetricData} contains the actual data.
     * 
     * @return Metrics mapped by the name of the metric.
     */
    Map<String, TracedMetricData> getUnscopedMetrics();

    /**
     * Returns the external requests associated with the transaction. If there were no external requests made, then null
     * will be returned.
     * 
     * @param transaction The name of the associated transaction.
     * @return All of the external requests made during the transaction for the input application.
     */
    Collection<ExternalRequest> getExternalRequests(String transaction);

    /**
     * Returns the data store requests associated with the input transaction. Returns null if there were no data store
     * requests made.
     * 
     * @param transaction The name of the associated transaction.
     * @return All of the data store requests made during the transaction for the input application.
     */
    Collection<DataStoreRequest> getDataStores(String transaction);

    /**
     * Returns all events collected. This excludes transaction events.
     * 
     * @return A collection of all events with the input type.
     */
    Collection<Event> getCustomEvents(String type);

    /**
     * Returns all custom event types collected. This excludes transaction events.
     * 
     * @return All custom event types collected.
     */
    Collection<String> getCustomEventTypes();

    /**
     * Returns all transaction events for the input transaction.
     * 
     * @param transactionName The name of the transaction of interest.
     * @return All transaction events for the input transaction.
     */
    Collection<TransactionEvent> getTransactionEvents(String transactionName);

    /**
     * Returns all errors that were collected since this was initialized. Returns null if there are no errors.
     * 
     * @return All errors collected since this was initialized or cleared.
     */
    Collection<Error> getErrors();

    /**
     * Returns all errors for the input transaction.
     * 
     * @param transactionName The name of the transaction of interest.
     * @return All errors reported for the input transaction since this was initialized or cleared.
     */
    Collection<Error> getErrorsForTransaction(String transactionName);

    /**
     * Returns all error events that were collected since this was initialized. Returns null if there are no errors.
     * 
     * @return All error events collected since this was initialized or cleared.
     */
    Collection<com.newrelic.agent.introspec.ErrorEvent> getErrorEvents();

    /**
     * Returns all error events for the input transaction.
     * 
     * @param transactionName The name of the transaction of interest.
     * @return All error events reported for the input transaction since this was initialized or cleared.
     */
    Collection<com.newrelic.agent.introspec.ErrorEvent> getErrorEventsForTransaction(String transactionName);

    /**
     * Returns all transaction traces associated with the input transaction name.
     * 
     * @param transactionName The name of the transaction of interest.
     * @return All transaction traces associated with the input transactionName.
     */
    Collection<TransactionTrace> getTransactionTracesForTransaction(String transactionName);

    /**
     * Returns the server port.
     *
     * @return the server port. null if not set.
     */
    Integer getServerPort();

    /**
     * Returns the dispatcher name.
     *
     * @return the dispatcher name.
     */
    String getDispatcher();

    /**
     * Returns the dispatcher version.
     *
     * @return the dispatcher version.
     */
    String getDispatcherVersion();

    /**
     * Returns all span events that were collected since this was initialized or cleared.
     *
     * @return collection of SpanEvents or null if there are none
     */
    Collection<SpanEvent> getSpanEvents();

    /**
     * Clear all existing SpanEvents
     */
    void clearSpanEvents();

    /**
     * Returns all log events that were collected since this was initialized or cleared.
     *
     * @return collection of LogEvents or null if there are none
     */
    Collection<LogEvent> getLogEvents();

    /**
     * Clear all existing LogEvents
     */
    void clearLogEvents();

    /**
     * Return random port available
     *
     * @return a port that's available
     */
    int getRandomPort();
}
