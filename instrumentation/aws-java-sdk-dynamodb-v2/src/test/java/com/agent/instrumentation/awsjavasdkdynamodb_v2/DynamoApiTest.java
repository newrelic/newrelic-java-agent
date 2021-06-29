/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdkdynamodb_v2;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"software.amazon.awssdk.services.dynamodb", "com.nr.instrumentation"})
public class DynamoApiTest {

    private static String hostName;
    private static DynamoDBProxyServer server;
    private static DynamoDbClient syncDynamoDbClient;
    private static DynamoDbAsyncClient asyncDynamoDbClient;
    private static String port;

    @BeforeClass
    public static void beforeClass() throws Exception {
        port = String.valueOf(InstrumentationTestRunner.getIntrospector().getRandomPort());
        hostName = InetAddress.getLocalHost().getHostName();
        server = ServerRunner.createServerFromCommandLineArgs(new String[]{"-inMemory", "-port", port});
        server.start();

        syncDynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .endpointOverride(new URI("http://localhost:" + port))
                .region(Region.US_WEST_1).build();

        asyncDynamoDbClient = DynamoDbAsyncClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .endpointOverride(new URI("http://localhost:" + port))
                .region(Region.US_WEST_1).build();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    private static final String DYNAMODB_PRODUCT = DatastoreVendor.DynamoDB.toString();
    private static final String TABLE_NAME = "test";
    private static final String SECOND_TABLE_NAME = "second_table";

    // Table used for async tests
    private static final String ASYNC_TABLE_NAME = "test-async";

    @Test
    public void testListAndCreateTable() {
        createTableTxn();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertScopedOperationMetricCount(txName, "listTables", 1);
        helper.assertScopedStatementMetricCount(txName, "createTable", TABLE_NAME, 1);
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Test
    public void testDescribeTable() {
        describeTableTxn();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertScopedStatementMetricCount(txName, "describeTable", TABLE_NAME, 1);
        helper.assertAggregateMetrics();
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Test
    public void testPutAndGetItem() {
        putAndGetTxn();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);

        helper.assertScopedStatementMetricCount(txName, "putItem", TABLE_NAME, 1);
        helper.assertScopedStatementMetricCount(txName, "getItem", TABLE_NAME, 1);
        helper.assertAggregateMetrics();
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Test
    public void testScanTable() {
        scanTableTxn();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);

        helper.assertScopedStatementMetricCount(txName, "scan", TABLE_NAME, 1);
        helper.assertAggregateMetrics();
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Test
    public void testDeleteTable() {
        deleteTableTxn();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertScopedStatementMetricCount(txName, "deleteTable", TABLE_NAME, 1);
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    //Async Client Tests

    @Test
    public void testListAndCreateTableAsync() throws ExecutionException, InterruptedException {
        createTableAsyncTxn();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertScopedOperationMetricCount(txName, "listTables", 1);
        helper.assertScopedStatementMetricCount(txName, "createTable", ASYNC_TABLE_NAME, 1);
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Test
    public void testDescribeTableASync() throws ExecutionException, InterruptedException {
        describeTableAsyncTxn();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertScopedStatementMetricCount(txName, "describeTable", ASYNC_TABLE_NAME, 1);
        helper.assertAggregateMetrics();
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Test
    public void testDeleteTableAsync() throws ExecutionException, InterruptedException {
        deleteTableAsyncTxn();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertScopedStatementMetricCount(txName, "deleteTable", ASYNC_TABLE_NAME, 1);
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Test
    public void testScanTableAsync() throws ExecutionException, InterruptedException {
        scanTableAsyncTxn();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);

        helper.assertScopedStatementMetricCount(txName, "scan", ASYNC_TABLE_NAME, 1);
        helper.assertAggregateMetrics();
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Test
    public void testPutGetDeleteItemAsync() throws ExecutionException, InterruptedException {
        putGetDeleteItemAsync();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);

        helper.assertScopedStatementMetricCount(txName, "putItem", ASYNC_TABLE_NAME, 1);
        helper.assertScopedStatementMetricCount(txName, "getItem", ASYNC_TABLE_NAME, 1);
        helper.assertScopedStatementMetricCount(txName, "deleteItem", ASYNC_TABLE_NAME, 1);
        helper.assertAggregateMetrics();
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    //sync test helpers

    @Trace(dispatcher = true)
    private void createTableTxn() {
        createTable(TABLE_NAME);
    }

    @Trace(dispatcher = true)
    private void listTablesTxn() {
        ListTablesRequest request = ListTablesRequest.builder().build();
        ListTablesResponse listTableResponse = syncDynamoDbClient.listTables(request);
    }

    @Trace(dispatcher = true)
    private void describeTableTxn() {
        createTable(TABLE_NAME);
        DescribeTableRequest request = DescribeTableRequest.builder()
                .tableName(TABLE_NAME)
                .build();
        syncDynamoDbClient.describeTable(request).table();
    }

    @Trace(dispatcher = true)
    private void putAndGetTxn() {
        createTable(TABLE_NAME);
        putItem();
        getItem();
    }

    @Trace(dispatcher = true)
    private void scanTableTxn() {
        createTable(TABLE_NAME);
        ScanRequest scanRequest = ScanRequest.builder().tableName(TABLE_NAME).build();
        syncDynamoDbClient.scan(scanRequest);
    }

    @Trace(dispatcher = true)
    private void deleteTableTxn() {
        createTable(TABLE_NAME);
        DeleteTableRequest request = DeleteTableRequest.builder().tableName(TABLE_NAME).build();
        syncDynamoDbClient.deleteTable(request);
    }

    private void getItem() {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Collections.singletonMap("artist", AttributeValue.builder().s("Pink").build()))
                .build();
        syncDynamoDbClient.getItem(request).item();
    }

    private void putItem() {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(createDefaultItem())
                .build();
        syncDynamoDbClient.putItem(request);
    }

    private Map<String, AttributeValue> createDefaultItem() {
        HashMap<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();

        // Add all content to the table
        itemValues.put("artist", AttributeValue.builder().s("Pink").build());
        itemValues.put("songTitle", AttributeValue.builder().s("lazy river").build());
        return itemValues;
    }

    private boolean tableExists(String table) {
        ListTablesRequest request = ListTablesRequest.builder().build();
        ListTablesResponse listTableResponse = syncDynamoDbClient.listTables(request);
        return listTableResponse.tableNames().contains(table);
    }

    private void createTable(String table) {
        if (tableExists(TABLE_NAME)) {
            return;
        }
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(table)
                .keySchema(KeySchemaElement.builder().attributeName("artist").keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName("artist").attributeType(ScalarAttributeType.S).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();
        syncDynamoDbClient.createTable(request);
    }

    //async test helpers
    @Trace(dispatcher = true)
    private void createTableAsyncTxn() throws ExecutionException, InterruptedException {
        createTableAsync();
    }

    @Trace(dispatcher = true)
    private void describeTableAsyncTxn() throws ExecutionException, InterruptedException {
        createTableAsync();
        DescribeTableRequest request = DescribeTableRequest.builder()
                .tableName(ASYNC_TABLE_NAME)
                .build();
        asyncDynamoDbClient.describeTable(request).get().table();
    }

    @Trace(dispatcher = true)
    private void deleteTableAsyncTxn() throws ExecutionException, InterruptedException {
        createTableAsync();
        DeleteTableRequest request = DeleteTableRequest.builder().tableName(ASYNC_TABLE_NAME).build();
        asyncDynamoDbClient.deleteTable(request).get();
    }

    @Trace(dispatcher = true)
    private void scanTableAsyncTxn() throws ExecutionException, InterruptedException {
        createTableAsync();
        ScanRequest scanRequest = ScanRequest.builder().tableName(ASYNC_TABLE_NAME).build();
        asyncDynamoDbClient.scan(scanRequest).get();
    }

    @Trace(dispatcher = true)
    private void putGetDeleteItemAsync() throws ExecutionException, InterruptedException {
        createTableAsync();
        putItemAsync();
        getItemAsync();
        deleteItemAsync();
    }

    private void getItemAsync() throws ExecutionException, InterruptedException {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(ASYNC_TABLE_NAME)
                .key(Collections.singletonMap("artist", AttributeValue.builder().s("Pink").build()))
                .build();
        asyncDynamoDbClient.getItem(request).get().item();
    }

    private void deleteItemAsync() throws ExecutionException, InterruptedException {
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(ASYNC_TABLE_NAME)
                .key(Collections.singletonMap("artist", AttributeValue.builder().s("Pink").build()))
                .build();
        asyncDynamoDbClient.deleteItem(request).get();
    }

    private void putItemAsync() throws ExecutionException, InterruptedException {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(ASYNC_TABLE_NAME)
                .item(createDefaultItem())
                .build();
        asyncDynamoDbClient.putItem(request).get();
    }

    private void createTableAsync() throws ExecutionException, InterruptedException {
        if (tableExistsAsync(ASYNC_TABLE_NAME)) {
            return;
        }
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(ASYNC_TABLE_NAME)
                .keySchema(KeySchemaElement.builder().attributeName("artist").keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName("artist").attributeType(ScalarAttributeType.S).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();
        asyncDynamoDbClient.createTable(request).get();
    }

    private boolean tableExistsAsync(String table) throws ExecutionException, InterruptedException {
        ListTablesRequest request = ListTablesRequest.builder().build();
        CompletableFuture<ListTablesResponse> listTableResponse = asyncDynamoDbClient.listTables(request);
        return listTableResponse.get().tableNames().contains(table);

    }
}
