/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.dispatchers;

import com.newrelic.agent.Transaction;

public abstract class DefaultDispatcher implements Dispatcher {

    private final Transaction transaction;
    private volatile boolean ignoreApdex = false;

    public DefaultDispatcher(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public boolean isIgnoreApdex() {
        return ignoreApdex;
    }

    public final void setIgnoreApdex(boolean ignore) {
        ignoreApdex = ignore;
    }

    protected boolean hasTransactionName(String transactionName, String rootMetricName) {
        return (transactionName != null && transactionName.length() != 0 && transactionName.indexOf(rootMetricName) == 0);
    }

    // always call hasTransactionName first - this method does not perform validity
    protected String getTransName(String transactionName, String rootMetricName, String secondSeg) {
        StringBuilder totalTimeName = new StringBuilder(transactionName.length() + secondSeg.length());
        totalTimeName.append(rootMetricName);
        totalTimeName.append(secondSeg);
        totalTimeName.append(transactionName.substring(rootMetricName.length()));
        return totalTimeName.toString();
    }

    protected String getApdexMetricName(String blameMetricName, String rootMetricName, String apdexMetricName) {
        if (blameMetricName != null && blameMetricName.indexOf(rootMetricName) == 0) {
            StringBuilder apdexName = new StringBuilder(apdexMetricName.length() + rootMetricName.length());
            apdexName.append(apdexMetricName);
            apdexName.append(blameMetricName.substring(rootMetricName.length()));
            return apdexName.toString();
        }
        return null;
    }

}
