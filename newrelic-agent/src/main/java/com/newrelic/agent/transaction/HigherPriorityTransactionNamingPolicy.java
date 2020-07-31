/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;

class HigherPriorityTransactionNamingPolicy extends TransactionNamingPolicy {

    HigherPriorityTransactionNamingPolicy() {
    }

    @Override
    public boolean canSetTransactionName(Transaction tx, TransactionNamePriority priority) {
        if (priority == null) {
            return false;
        }
        PriorityTransactionName ptn = tx.getPriorityTransactionName();
        return TransactionNamingUtility.comparePriority(priority, ptn.getPriority(), tx.getNamingScheme()) > 0;
    }

}
