/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import java.util.Map;

import static com.newrelic.agent.introspec.MetricsHelper.getScopedMetricCount;
import static com.newrelic.agent.introspec.MetricsHelper.getUnscopedMetricCount;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Utility class to help make datastore assertions in JUnit test cases run with an InstrumentationTestRunner. This class
 * only works with a single datastore product or vendor; do not use if your transaction contains datastore metrics from
 * multiple products.
 */
public class DatastoreHelper {
    private final String product;

    /**
     * Create a DatastoreHelper for the specified datastore product or vendor name.
     *
     * @param product datastore product or vendor name
     */
    public DatastoreHelper(String product) {
        this.product = product;
    }

    /**
     * Assert that aggregate datastore metrics were generated properly.
     */
    public void assertAggregateMetrics() {
        // check unscoped non-vendor metric counts
        int datastoreAllMetrics = getUnscopedMetricCount("Datastore/all");
        assertTrue("No Datastore/all metrics found.", datastoreAllMetrics > 0);

        int datastoreAllWebMetrics = getUnscopedMetricCount("Datastore/allWeb");
        int datastoreAllOtherMetrics = getUnscopedMetricCount("Datastore/allOther");
        assertEquals(datastoreAllMetrics, datastoreAllWebMetrics + datastoreAllOtherMetrics);

        // check vendor-specific counts
        int datastoreVendorAllMetrics = getUnscopedMetricCount("Datastore/" + product + "/all");
        int datastoreVendorAllWebMetrics = getUnscopedMetricCount("Datastore/" + product + "/allWeb");
        int datastoreVendorAllOtherMetrics = getUnscopedMetricCount("Datastore/" + product + "/allOther");
        assertEquals(datastoreVendorAllMetrics, datastoreVendorAllWebMetrics + datastoreVendorAllOtherMetrics);
        assertEquals(datastoreAllMetrics, datastoreVendorAllMetrics);
    }

    /**
     * Get the datastore operation metric name for the specified operation.
     *
     * @param operation datastore operation
     * @return datastore operation metric name
     */
    public String getOperationMetricName(String operation) {
        return "Datastore/operation/" + product + "/" + operation;
    }

    /**
     * Assert the unscoped metric call count for the operation is the same as the specified count.
     *
     * @param operation datastore operation
     * @param count expected metric call count
     */
    public void assertUnscopedOperationMetricCount(String operation, int count) {
        String operationMetricName = getOperationMetricName(operation);
        String message = String.format("Unscoped metric count of %s", operationMetricName);
        assertEquals(message, count, getUnscopedMetricCount(operationMetricName));
    }

    /**
     * Assert the scoped metric call count for the operation is the same as the specified count.
     *
     * @param txName transaction name
     * @param operation datastore operation
     * @param count expected metric call count
     */
    public void assertScopedOperationMetricCount(String txName, String operation, int count) {
        String operationMetricName = getOperationMetricName(operation);
        String message = String.format("Metric count of %s scoped to %s", operationMetricName, txName);
        int scopedOperationMetrics = getScopedMetricCount(txName, operationMetricName);
        assertEquals(message, count, scopedOperationMetrics);
    }

    /**
     * Get the datastore statement metric name for the specified product, operation, and collection
     *
     * @param operation datastore operation
     * @param collection datastore collection or table name
     * @return datastore statement metric name
     */
    public String getStatementMetricName(String operation, String collection) {
        return "Datastore/statement/" + product + "/" + collection + "/" + operation;
    }

    /**
     * Assert the unscoped metric call count for the statement is the same as the specified count.
     *
     * @param operation datastore operation
     * @param collection datastore collection or table name
     * @param count expected metric call count
     */
    public void assertUnscopedStatementMetricCount(String operation, String collection, int count) {
        String operationMetricName = getOperationMetricName(operation);
        int operationMetrics = getUnscopedMetricCount(operationMetricName);
        if (count > 0) {
            String message = String.format("Unscoped metric count of %s should be greater than zero but is %d",
                    operationMetricName, operationMetrics);
            assertTrue(message, operationMetrics > 0);
        }

        String statementMetricName = getStatementMetricName(operation, collection);
        int statementMetrics = getUnscopedMetricCount(statementMetricName);

        String message = String.format(
                "Operation metric %s: %d should be greater than or equal to the statement metric %s: %d",
                operationMetricName, operationMetrics, statementMetricName, statementMetrics);
        assertTrue(message, operationMetrics >= statementMetrics);

        message = String.format("Unscoped metric count of %s", statementMetricName);
        assertEquals(message, count, statementMetrics);
    }

    /**
     * Assert the scoped metric call count for the statement is the same as the specified count.
     *
     * @param txName transaction name
     * @param operation datastore operation
     * @param collection datastore collection or table name
     * @param count expected metric call count
     */
    public void assertScopedStatementMetricCount(String txName, String operation, String collection, int count) {
        String statementMetricName = getStatementMetricName(operation, collection);
        String message = String.format("Metric count of %s scoped to %s", statementMetricName, txName);
        assertEquals(message, count, getScopedMetricCount(txName, statementMetricName));
    }

    /**
     * Convenience method to assert that the unscoped operation metric, unscoped statement metric, and scoped statement
     * metric all have the specified count. Equivalent to calling:
     * <ul>
     * <li>{@link #assertUnscopedOperationMetricCount(String, int)}</li>
     * <li>{@link #assertUnscopedStatementMetricCount(String, String, int)}</li>
     * <li>{@link #assertScopedStatementMetricCount(String, String, String, int)}</li>
     * </ul>
     *
     * @param txName transaction name
     * @param operation datastore operation
     * @param collection datastore collection or table name
     * @param count expected metric call count
     */
    public void assertUnifiedMetricCounts(String txName, String operation, String collection, int count) {
        assertUnscopedOperationMetricCount(operation, count);
        assertUnscopedStatementMetricCount(operation, collection, count);
        assertScopedStatementMetricCount(txName, operation, collection, count);
    }

    public void assertInstanceLevelMetric(String dbVendor, String host, String portOrIdentifier) {
        Map<String, TracedMetricData> unscopedMetrics = InstrumentationTestRunner.getIntrospector().getUnscopedMetrics();
        String instanceMetric = com.newrelic.agent.database.DatastoreMetrics.buildInstanceMetric(dbVendor, host, null,
                portOrIdentifier);
        assertTrue("Instance metric " + instanceMetric + " not found. Other instance metrics found: "
                + otherInstanceMetrics(), unscopedMetrics.containsKey(instanceMetric));
    }

    private String otherInstanceMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = InstrumentationTestRunner.getIntrospector().getUnscopedMetrics();
        StringBuilder sb = new StringBuilder();
        for (String metric : unscopedMetrics.keySet()) {
            if (metric.startsWith("Datastore/instance")) {
                sb.append(metric);
                sb.append(", ");
            }

        }
        return sb.toString();
    }
}
