/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdkdynamodb_v2;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"software.amazon.awssdk.services.dynamodb", "com.nr.instrumentation"})
public class DefaultDynamoDbClient_InstrumentationTest {

    private static final String DYNAMODB_PRODUCT = DatastoreVendor.DynamoDB.toString();
    private static final String TABLE_NAME = "test";
    private static final long DEFAULT_TIMEOUT_IN_MILLIS = 10_000;
    private static LocalTestDynamoDb dynamoDb;

    @BeforeClass
    public static void beforeClass() throws Exception {
        dynamoDb = LocalTestDynamoDb.create();
        dynamoDb.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        dynamoDb.stop();
    }

    @Before
    public void beforeEach() {
        if (!dynamoDb.tableExists()) {
            dynamoDb.createTable();
        }
    }

    @After
    public void afterEach() {
        if (dynamoDb.tableExists()) {
            dynamoDb.deleteTable();
        }
    }

    @Test
    public void testDescribeTimeToLive() {
        // when
        trace(dynamoDb::describeTimeToLive);
        // then
        assertTableOperation("describeTimeToLive");
    }

    @Test
    public void testDescribeTimeToLiveAsync() {
        // when
        trace(dynamoDb::describeTimeToLiveAsync);
        // then
        assertTableOperation("describeTimeToLive");
    }

    @Test
    public void testDescribeLimits() {
        // when
        trace(dynamoDb::describeLimits);
        // then
        assertOperation("describeLimits");
    }

    @Test
    public void testDescribeLimitsAsync() {
        // when
        trace(dynamoDb::describeLimitsAsync);
        // then
        assertOperation("describeLimits");
    }

    @Test
    public void testUpdateTable() {
        // when
        trace(dynamoDb::updateTable);
        // then
        assertTableOperation("updateTable");
    }

    @Test
    public void testUpdateTableAsync() {
        // when
        trace(dynamoDb::updateTableAsync);
        // then
        assertTableOperation("updateTable");
    }

    @Test
    public void testUpdateTimeToLive() {
        // when
        trace(dynamoDb::updateTimeToLive);
        // then
        assertTableOperation("updateTimeToLive");
    }

    @Test
    public void testUpdateTimeToLiveAsync() {
        // when
        trace(dynamoDb::updateTimeToLiveAsync);
        // then
        assertTableOperation("updateTimeToLive");
    }

    @Test
    public void testBatchGetItem() {
        // when
        trace(dynamoDb::batchGetItem);
        // then
        assertScopedStatementMetric("batchGetItem", "batch");
    }

    @Test
    public void testBatchGetItemAsync() {
        // when
        trace(dynamoDb::batchGetItemAsync);
        // then
        assertScopedStatementMetric("batchGetItem", "batch");
    }

    @Test
    public void testBatchWriteItem() {
        // when
        trace(dynamoDb::batchWriteItem);
        // then
        assertScopedStatementMetric("batchWriteItem", "batch");
    }

    @Test
    public void testBatchWriteItemAsync() {
        // when
        trace(dynamoDb::batchWriteItemAsync);
        // then
        assertScopedStatementMetric("batchWriteItem", "batch");
    }

    @Test
    public void testCreateTable() {
        // given: table does not exist
        dynamoDb.deleteTable();
        // when
        trace(dynamoDb::createTable);
        // then
        assertTableOperation("createTable");
    }

    @Test
    public void testCreateTableAsync() {
        // given: table does not exist
        dynamoDb.deleteTable();
        // when
        trace(dynamoDb::createTableAsync);
        // then
        assertTableOperation("createTable");
    }

    @Test
    public void testPutGetUpdateDeleteItem() {
        // when
        trace(new Runnable[]{
                dynamoDb::putItem,
                dynamoDb::getItem,
                dynamoDb::updateItem,
                dynamoDb::deleteItem
        });
        // then
        assertTableOperations(new String[]{"putItem", "getItem", "updateItem", "deleteItem"});
    }

    @Test
    public void testPutGetUpdateDeleteItemAsync() {
        // when
        trace(new Runnable[]{
                dynamoDb::putItemAsync,
                dynamoDb::getItemAsync,
                dynamoDb::updateItemAsync,
                dynamoDb::deleteItemAsync
        });
        // then
        assertTableOperations(new String[]{"putItem", "getItem", "updateItem", "deleteItem"});
    }

    @Test
    public void testQuery() {
        // when
        trace(dynamoDb::query);
        // then
        assertTableOperation("query");
    }

    @Test
    public void testQueryAsync() {
        // when
        trace(dynamoDb::queryAsync);
        // then
        assertTableOperation("query");
    }

    @Test
    public void testListTables() {
        // when
        trace(dynamoDb::listTables);
        // then
        assertOperation("listTables");
    }

    @Test
    public void testDescribeTable() {
        // when
        trace(dynamoDb::describeTable);
        // then
        assertTableOperation("describeTable");
    }

    @Test
    public void testDescribeTableAsync() {
        // when
        trace(dynamoDb::describeTableAsync);
        // then
        assertTableOperation("describeTable");
    }

    @Test
    public void testScan() {
        // when
        trace(dynamoDb::scan);
        // then
        assertTableOperation("scan");
    }

    @Test
    public void testScanAsync() {
        // when
        trace(dynamoDb::scanAsync);
        // then
        assertTableOperation("scan");
    }

    @Test
    public void testDeleteTable() {
        // when
        trace(dynamoDb::deleteTable);
        // then
        assertTableOperation("deleteTable");
    }

    @Trace(dispatcher = true)
    private void trace(Runnable runnable) {
        runnable.run();
    }

    @Trace(dispatcher = true)
    private void trace(Runnable[] actions) {
        Arrays.stream(actions).forEach(Runnable::run);
    }

    private void assertOperation(String operation) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_IN_MILLIS));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertAggregateMetrics();
        helper.assertScopedOperationMetricCount(txName, operation, 1);
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, dynamoDb.getHostName(), dynamoDb.getPort());
    }

    private void assertScopedStatementMetric(String operation, String collection) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_IN_MILLIS));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertAggregateMetrics();
        helper.assertScopedStatementMetricCount(txName, operation, collection, 1);
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, dynamoDb.getHostName(), dynamoDb.getPort());
    }

    private void assertTableOperation(String operation) {
        assertScopedStatementMetric(operation, TABLE_NAME);
    }

    private void assertTableOperations(String[] operations) {
        Arrays.stream(operations).forEach(this::assertTableOperation);
    }
}
