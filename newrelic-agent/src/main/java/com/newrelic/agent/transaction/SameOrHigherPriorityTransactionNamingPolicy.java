/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;

import java.util.logging.Level;

class SameOrHigherPriorityTransactionNamingPolicy extends TransactionNamingPolicy {

    SameOrHigherPriorityTransactionNamingPolicy() {
    }

    @Override
    public boolean canSetTransactionName(Transaction tx, TransactionNamePriority priority) {
        if (priority == null) {
            return false;
        }

        Agent.LOG.log(Level.FINEST, "agent.transaction.SameOrHigherPriorityTransactionNamingPolicy::canSetTransactionName: " +
                        "txn: {0}, txnPriorityTxnName: {1}, txnNamingScheme: {2},  priority: {3}",
                tx.toString(), tx.getPriorityTransactionName().toString(), tx.getNamingScheme().toString(), priority.toString());

        PriorityTransactionName ptn = tx.getPriorityTransactionName();
        return TransactionNamingUtility.comparePriority(priority, ptn.getPriority(), tx.getNamingScheme()) >= 0;
    }

}
