/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import java.text.MessageFormat;

import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;

/**
 * Send old Database metrics.
 *
 * This will be removed when the Datastore UI is ready.
 */
public class LegacyDatabaseMetrics {
    public static final String METRIC_NAMESPACE = "Database";
    public static final String ALL = METRIC_NAMESPACE + "/all";
    public static final String ALL_WEB = METRIC_NAMESPACE + "/allWeb";
    public static final String ALL_OTHER = METRIC_NAMESPACE + "/allOther";

    public static final String STATEMENT = METRIC_NAMESPACE + "/{0}/{1}"; // Database/myTable/insert
    public static final String OPERATION = METRIC_NAMESPACE + "/{0}"; // Database/insert

    public static void doDatabaseMetrics(Transaction tx, TracedMethod method, String table, String operation) {
        method.setMetricName(MessageFormat.format(STATEMENT, table, operation));

        method.addRollupMetricName(MessageFormat.format(STATEMENT, table, operation));
        method.addRollupMetricName(MessageFormat.format(OPERATION, operation));

        method.addRollupMetricName(ALL);
        if (tx.isWebTransaction()) {
            method.addRollupMetricName(ALL_WEB);
        } else {
            method.addRollupMetricName(ALL_OTHER);
        }
    }

}
