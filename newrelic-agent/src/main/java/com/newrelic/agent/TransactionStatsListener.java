/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.stats.TransactionStats;

/**
 * Allows objects to be notified when transaction stats are finished processing. This happens after
 * {@link TransactionListener}'s have been executed.
 */
public interface TransactionStatsListener {

    /**
     * Called after a dispatcher transaction finishes processing its stats
     *
     * @param transactionData the final data from the transaction
     * @param transactionStats the final metric information from the transaction
     */
    void dispatcherTransactionStatsFinished(TransactionData transactionData, TransactionStats transactionStats);

}
