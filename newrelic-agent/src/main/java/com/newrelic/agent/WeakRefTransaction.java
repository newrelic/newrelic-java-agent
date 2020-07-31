/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.lang.ref.WeakReference;

public class WeakRefTransaction extends TransactionApiImpl {

    private WeakReference<com.newrelic.agent.Transaction> transactionWeakReference;

    protected WeakRefTransaction(com.newrelic.agent.Transaction internalTxn) {
        transactionWeakReference = new WeakReference<>(internalTxn);
    }

    @Override
    protected Transaction getTransactionIfExists() {
        return transactionWeakReference.get();
    }

}
