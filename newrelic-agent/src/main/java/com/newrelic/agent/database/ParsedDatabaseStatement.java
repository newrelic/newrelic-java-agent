/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.UnknownDatabaseVendor;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

/**
 * The result of parsing a sql statement. This class is immutable.
 */
public final class ParsedDatabaseStatement implements MetricNameFormat {
    private final String operation;
    private final String model;
    private final boolean generateMetric;
    private final String metricName;
    private final String operationRollupMetricName;
    private final DatabaseVendor dbVendor;

    public ParsedDatabaseStatement(String model, String operation, boolean generateMetric) {
        this(UnknownDatabaseVendor.INSTANCE, model, operation, generateMetric);
    }

    public ParsedDatabaseStatement(DatabaseVendor dbVendor, String model, String operation, boolean generateMetric) {
        this.model = model;
        this.operation = operation;
        this.generateMetric = generateMetric;
        this.dbVendor = dbVendor;

        operationRollupMetricName = new StringBuilder(DatastoreMetrics.OPERATION).append(dbVendor.getDatastoreVendor())
                                                                                 .append(DatastoreMetrics.SLASH)
                                                                                 .append(operation).toString();
        if (model == null || model.isEmpty()) {
            metricName = operationRollupMetricName;
        } else {
            metricName = new StringBuilder(DatastoreMetrics.STATEMENT).append(dbVendor.getDatastoreVendor())
                                                                      .append(DatastoreMetrics.SLASH).append(model)
                                                                      .append(DatastoreMetrics.SLASH).append(operation)
                                                                      .toString();
        }
    }

    public String getOperation() {
        return operation;
    }

    public String getModel() {
        return model;
    }

    public DatabaseVendor getDbVendor() {
        return dbVendor;
    }

    /**
     * Returns true if a metric should be recorded for this statement.
     *
     */
    public boolean recordMetric() {
        return generateMetric;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }

    @Override
    public String toString() {
        return new StringBuilder(operation).append(' ').append(model).toString();
    }

    @Override
    public String getTransactionSegmentName() {
        return getMetricName();
    }

    public String getOperationRollupMetricName() {
        return operationRollupMetricName;
    }

    @Override
    public String getTransactionSegmentUri() {
        return null;
    }
}
