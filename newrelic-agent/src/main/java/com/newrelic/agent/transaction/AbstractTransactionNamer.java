/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;

public abstract class AbstractTransactionNamer implements TransactionNamer {

    private final Transaction tx;
    private final String uri;

    protected AbstractTransactionNamer(Transaction tx, String uri) {
        this.tx = tx;
        this.uri = uri;
    }

    protected final String getUri() {
        return uri;
    }

    protected final Transaction getTransaction() {
        return tx;
    }

    protected boolean canSetTransactionName() {
        return canSetTransactionName(TransactionNamePriority.REQUEST_URI);
    }

    protected boolean canSetTransactionName(TransactionNamePriority priority) {
        if (tx == null || tx.isIgnore()) {
            return false;
        }
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        return (policy.canSetTransactionName(tx, priority));
    }

    protected void setTransactionName(String name, String category, TransactionNamePriority priority) {
        if (canSetTransactionName(priority)) {
            TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
            policy.setTransactionName(tx, name, category, priority);
        }
    }
}
