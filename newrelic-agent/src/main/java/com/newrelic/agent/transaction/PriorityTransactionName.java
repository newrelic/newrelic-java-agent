/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;

/**
 * A transaction name that participates in the priority name assignment rules. This class is immutable and therefore
 * threadsafe.
 */
public class PriorityTransactionName {

    public static final PriorityTransactionName NONE = PriorityTransactionName.create((String) null, null,
            TransactionNamePriority.NONE);
    public static final String WEB_TRANSACTION_CATEGORY = "Web";
    public static final String UNDEFINED_TRANSACTION_CATEGORY = "Other";

    private final TransactionNamePriority priority;
    private final String prefix;
    private final String partialName;
    private final String category;
    private final String cachedName;

    private PriorityTransactionName(String prefix, String partialName, String category, TransactionNamePriority priority) {
        this.prefix = prefix == null ? getPrefix() : prefix;
        this.partialName = partialName;
        this.category = category;
        this.priority = priority;
        this.cachedName = this.prefix + partialName;
    }

    private String initializeName(String partialName) {
        String prefix = getPrefix();
        if (prefix == null) {
            return null;
        }
        if (partialName == null) {
            return prefix;
        }
        if (!prefix.equals(this.prefix)) {
            return prefix + partialName;
        } else {
            return cachedName;
        }
    }

    public String getName() {
        return initializeName(partialName);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPartialName() {
        return partialName;
    }

    public String getCategory() {
        return category;
    }

    public boolean isFrozen() {
        return priority == TransactionNamePriority.FROZEN;
    }

    public PriorityTransactionName freeze() {
        Agent.LOG.log(Level.FINEST, "Setting priority transaction name to FROZEN: {0}", toString());
        if (isFrozen()) {
            return this;
        }
        return PriorityTransactionName.create(getPrefix(), getPartialName(), category, TransactionNamePriority.FROZEN);
    }

    public TransactionNamePriority getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}[name={1}, priority={2}]", getClass().getName(), getName(), getPriority());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((partialName == null) ? 0 : partialName.hashCode());
        result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
        result = prime * result + ((priority == null) ? 0 : priority.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PriorityTransactionName)) {
            return false;
        }
        PriorityTransactionName other = (PriorityTransactionName) obj;
        String name = getName();
        String otherName = other.getName();
        if (name == null) {
            if (otherName != null) {
                return false;
            }
        } else if (!name.equals(otherName)) {
            return false;
        }
        return priority.equals(other.priority);
    }

    public static PriorityTransactionName create(String transactionName, String category,
            TransactionNamePriority priority) {
        if (transactionName == null) {
            return new PriorityTransactionName(null, null, category, priority);
        }
        int index = transactionName.indexOf(MetricNames.SEGMENT_DELIMITER, 1);
        if (index > 0) {
            index = transactionName.indexOf(MetricNames.SEGMENT_DELIMITER, index + 1);
            if (index > 0) {
                String prefix = transactionName.substring(0, index);
                String partialName = transactionName.substring(index);
                return new PriorityTransactionName(prefix, partialName, category, priority);
            }
        }
        return new PriorityTransactionName(transactionName, null, category, priority);
    }

    public static PriorityTransactionName create(final Transaction tx, String partialName, String category,
            TransactionNamePriority priority) {
        if (priority == null) {
            return null;
        }
        if (category == null || category.isEmpty()) {
            category = tx.isWebTransaction() ? WEB_TRANSACTION_CATEGORY : UNDEFINED_TRANSACTION_CATEGORY;
        }
        return new PriorityTransactionName(null, partialName, category, priority) {

            @Override
            public String getPrefix() {
                return tx.isWebTransaction() ? MetricNames.WEB_TRANSACTION : MetricNames.OTHER_TRANSACTION;
            }

        };
    }

    public static PriorityTransactionName create(String prefix, String partialName, String category,
            TransactionNamePriority priority) {
        if (priority == null) {
            return null;
        }
        return new PriorityTransactionName(prefix, partialName, category, priority);
    }
}
