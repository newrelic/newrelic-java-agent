/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * Allows objects to be notified when transactions start or fail to finish due to an unexpected error.
 */
public interface ExtendedTransactionListener extends TransactionListener {

    /**
     * Called after a dispatcher transaction is started on the request thread.
     *
     * @param transaction The transaction that was just started
     */
    void dispatcherTransactionStarted(Transaction transaction);

    /**
     * The transaction on the current thread is being cancelled, either because of a serious internal error or because
     * the activity on the current thread is now linked to another transaction and this transaction should not report.
     * 
     * @param transaction the transaction that is completing abruptly.
     */
    void dispatcherTransactionCancelled(Transaction transaction);

}
