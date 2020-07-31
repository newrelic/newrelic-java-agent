/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;

/**
 * {@link TransactionNamer} implementation for other transactions.
 */
public class OtherTransactionNamer extends AbstractTransactionNamer {

    private OtherTransactionNamer(Transaction tx, String dispatcherUri) {
        super(tx, dispatcherUri);
    }

    @Override
    public void setTransactionName() {
        setTransactionName(getUri(), "", TransactionNamePriority.REQUEST_URI);
    }

    public static TransactionNamer create(Transaction tx, String dispatcherUri) {
        return new OtherTransactionNamer(tx, dispatcherUri);
    }
}
