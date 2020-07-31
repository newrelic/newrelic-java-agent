/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.stats.TransactionStats;

/**
 * Allows objects to be notified when transactions start, finish or fail to finish due to an unexpected error.
 */
public interface TransactionListener {

    /**
     * Called after a dispatcher transaction finishes on the request thread.
     *
     * @param transactionData the final data from the transaction
     * @param transactionStats the final metric information from the transaction
     */
    void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats);

}
