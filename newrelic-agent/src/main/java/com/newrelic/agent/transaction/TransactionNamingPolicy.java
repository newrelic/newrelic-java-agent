/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import org.apache.commons.lang3.StringUtils;

public abstract class TransactionNamingPolicy {
    private static final HigherPriorityTransactionNamingPolicy HIGHER_PRIORITY_INSTANCE = new HigherPriorityTransactionNamingPolicy();
    private static final SameOrHigherPriorityTransactionNamingPolicy SAME_OR_HIGHER_INSTANCE = new SameOrHigherPriorityTransactionNamingPolicy();

    public final boolean setTransactionName(Transaction tx, String name, String category,
            TransactionNamePriority priority) {
        return tx.conditionalSetPriorityTransactionName(this, name, category, priority);
    }

    public abstract boolean canSetTransactionName(Transaction tx, TransactionNamePriority priority);

    public PriorityTransactionName getPriorityTransactionName(Transaction tx, String name, String category,
            TransactionNamePriority priority) {
        if (category == null) {
            return PriorityTransactionName.create(name, category, priority);
        }
        if (name == null) {
            return PriorityTransactionName.create(name, category, priority);
        }
        String txType = tx.isWebTransaction() ? MetricNames.WEB_TRANSACTION : MetricNames.OTHER_TRANSACTION;
        if (!StringUtils.isEmpty(name)) {
            if (name.startsWith(txType)) {
                return PriorityTransactionName.create(name, category, priority);
            }
            if (!name.startsWith(MetricNames.SEGMENT_DELIMITER_STRING)) {
                name = MetricNames.SEGMENT_DELIMITER + name;
            }
        }
        if (category.length() > 0) {
            name = MetricNames.SEGMENT_DELIMITER + category + name;
        }

        return PriorityTransactionName.create(tx, name, category, priority);
    }

    public static TransactionNamingPolicy getSameOrHigherPriorityTransactionNamingPolicy() {
        return SAME_OR_HIGHER_INSTANCE;
    }

    public static TransactionNamingPolicy getHigherPriorityTransactionNamingPolicy() {
        return HIGHER_PRIORITY_INSTANCE;
    }

}
