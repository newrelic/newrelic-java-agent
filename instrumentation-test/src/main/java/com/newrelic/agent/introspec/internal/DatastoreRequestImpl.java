/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.introspec.DataStoreRequest;

class DatastoreRequestImpl extends RequestImpl implements DataStoreRequest {

    // there are two options for these
    // Datastore/statement/datastore/table/operation_name
    // Datastore/operation/datastore/operation_name

    private static Pattern DATASTORE_STATEMENT = Pattern.compile("Datastore/statement/([^/]+)/([^/]+)/([^/]+)");
    private static Pattern DATASTORE_OPERATION = Pattern.compile("Datastore/operation/([^/]+)/([^/]+)");

    private String tableName;
    private String operationName;

    private DatastoreRequestImpl(String originalMetric, String datastore, String table, String operation) {
        super(originalMetric, datastore);
        tableName = table;
        operationName = operation;
    }

    private DatastoreRequestImpl(String originalMetric, String datastore, String operation) {
        super(originalMetric, datastore);
        operationName = operation;
    }

    public static DatastoreRequestImpl checkAndMakeDatastore(String metricName) {

        Matcher matcher = DATASTORE_STATEMENT.matcher(metricName);
        if (matcher.matches() && matcher.groupCount() == 3) {
            return new DatastoreRequestImpl(metricName, matcher.group(1), matcher.group(2), matcher.group(3));
        } else {
            matcher = DATASTORE_OPERATION.matcher(metricName);
            if (matcher.matches() && matcher.groupCount() == 2) {
                return new DatastoreRequestImpl(metricName, matcher.group(1), matcher.group(2));
            }
        }
        return null;
    }

    public String getDatastore() {
        return getHostname();
    }

    public String getTable() {
        return tableName;
    }

    public String getOperation() {
        return operationName;
    }
}
