/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import com.newrelic.agent.bridge.TransactionNamePriority;

public class TransactionName {

    public static final TransactionName CUSTOM_DEFAULT = new TransactionName(TransactionNamePriority.CUSTOM_HIGH);
    public static final TransactionName BUILT_IN_DEFAULT = new TransactionName(TransactionNamePriority.FRAMEWORK_HIGH);

    final String category;
    final String path;
    final TransactionNamePriority transactionNamePriority;
    final boolean override;

    public TransactionName(TransactionNamePriority namingPriority, boolean override, String category, String path) {
        this.category = category;
        this.path = path;
        this.override = override;
        this.transactionNamePriority = namingPriority;
    }

    private TransactionName(TransactionNamePriority priority) {
        this(priority, false, null, null);
    }

    public static boolean isSimpleTransactionName(TransactionName transactionName) {
        return BUILT_IN_DEFAULT.equals(transactionName) || CUSTOM_DEFAULT.equals(transactionName);
    }
}
