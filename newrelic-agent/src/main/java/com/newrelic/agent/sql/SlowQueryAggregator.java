/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import java.util.List;

import com.newrelic.agent.TransactionData;

/**
 * This class is responsible for aggregating similar slow queries across transactions
 */
public interface SlowQueryAggregator {

    /**
     * Returns and resets a list of slow queries since the last time this method was called.
     * 
     * @return a list of slow queries since the last time this method was called
     */
    List<SqlTrace> getAndClearSlowQueries();

    /**
     * Add any slow queries that occurred during the provided transaction.
     * 
     * @param td the transaction to check for slow query traces
     */
    void addSlowQueriesFromTransaction(TransactionData td);

}
