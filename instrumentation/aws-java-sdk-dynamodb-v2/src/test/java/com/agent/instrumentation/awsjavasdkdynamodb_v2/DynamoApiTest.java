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
    public void testCreateTable() {
        createTableTxn();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertScopedStatementMetricCount(txName, "createTable", TABLE_NAME, 1);
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Test
    public void testListTable() {
        listTablesTxn();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(10000));

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertScopedOperationMetricCount(txName, "listTables", 1);
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
//        putItem();
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
        if (!tableExists(TABLE_NAME)) {
            createTable(TABLE_NAME);
        }
        DescribeTableRequest request = DescribeTableRequest.builder()
                .tableName(TABLE_NAME)
                .build();
        syncDynamoDbClient.describeTable(request).table();
    }

    @Trace(dispatcher = true)
    private void putAndGetTxn() {
        if (!tableExists(TABLE_NAME)) {
            createTable(TABLE_NAME);
        }
        putItem();
        getItem();
    }

    @Trace(dispatcher = true)
    private void scanTableTxn() {
        if (!tableExists(TABLE_NAME)) {
            createTable(TABLE_NAME);
        }
        ScanRequest scanRequest = ScanRequest.builder().tableName(TABLE_NAME).build();
        syncDynamoDbClient.scan(scanRequest);
    }

    @Trace(dispatcher = true)
    private void deleteTableTxn() {
        if (!tableExists(TABLE_NAME)) {
            createTable(TABLE_NAME);
        }
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
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(table)
                .keySchema(KeySchemaElement.builder().attributeName("artist").keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName("artist").attributeType(ScalarAttributeType.S).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();
        syncDynamoDbClient.createTable(request);
    }

//    @Test
//    public void testDescribeTable() {
//        describeTableTxn();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//        helper.assertScopedStatementMetricCount(txName, "describeTable", TABLE_NAME, 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testPutAndGetItem() {
//        putAndGetTxn();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "putItem", TABLE_NAME, 1);
//        helper.assertScopedStatementMetricCount(txName, "getItem", TABLE_NAME, 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testPutAndGetItemAsync() throws ExecutionException, InterruptedException {
//        putAndGetTxnAsync();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "putItem", TABLE_NAME, 1);
//        helper.assertScopedStatementMetricCount(txName, "getItem", TABLE_NAME, 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testQueryTable() {
//        putItem();
//        queryTableTxn();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "query", TABLE_NAME, 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testScanTable() {
//        putItem();
//        scanTableTxn();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "scan", "test", 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testScanTableAsync() throws ExecutionException, InterruptedException {
//        putItem();
//        scanTableTxnAsync();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(3000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "scan", "test", 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testUpdateItem() {
//        putItem();
//        UpdateItemResult result = updateItemTxn();
//
//        assertNotNull(result);
//
//        Map<String, AttributeValue> item = getItem().getItem();
//        AttributeValue attr = item.get("rating");
//        assertNotNull(attr);
//        assertEquals("{S: 5 stars,}", attr.toString());
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "updateItem", "test", 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testDeleteTable() {
//        deleteTableTxn();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//        helper.assertScopedStatementMetricCount(txName, "deleteTable", TABLE_NAME, 1);
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testBatchWrite() {
//        getOrCreateTable(TABLE_NAME);
//        getOrCreateTable(SECOND_TABLE_NAME);
//
//        batchWriteTxn();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "batchWriteItem", "batch", 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testBatchGetAsync() throws ExecutionException, InterruptedException {
//        getOrCreateTable(TABLE_NAME);
//        batchGetAsyncTxn();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "batchGetItem", "batch", 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testBatchPutAsync() {
//        getOrCreateTable(TABLE_NAME);
//        batchPutTxnAsync();
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "batchWriteItem", "batch", 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testBadGetAsync() {
//        getOrCreateTable(TABLE_NAME);
//        getWrongItemAsyncTxn();
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
//        TransactionEvent event = transactionEvents.iterator().next();
//        Map<String, Object> attributes = event.getAttributes();
//
//        assertEquals(5, attributes.get("Miley Cyrus is not jazz"));
//        assertEquals(100, attributes.get("Mingus is totally jazz"));
//
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//
//        helper.assertScopedStatementMetricCount(txName, "getItem", "test", 2);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testAsyncParenting() throws ExecutionException, InterruptedException {
//        batchGetAsyncTxn();
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//        String txName = introspector.getTransactionNames().iterator().next();
//        TransactionTrace trace = introspector.getTransactionTracesForTransaction(txName).iterator().next();
//        TraceSegment initialTraceSegment = trace.getInitialTraceSegment();
//
//        assertEquals("batchGetAsyncTxn", initialTraceSegment.getMethodName());
//        assertEquals("batchGetItemAsync", initialTraceSegment.getChildren().get(0).getMethodName());
//    }
//
//    @Trace(dispatcher = true)
//    private void batchGetAsyncTxn() throws ExecutionException, InterruptedException {
//        Map<String, AttributeValue> key1 = new HashMap<>();
//        Map<String, AttributeValue> key2 = new HashMap<>();
//
//        key1.put("artist", new AttributeValue("Charles Mingus"));
//        key1.put("year", new AttributeValue().withN("1959"));
//
//        key2.put("artist", new AttributeValue("Eric Dolphy"));
//        key2.put("year", new AttributeValue().withN("1960"));
//
//        KeysAndAttributes keysAndAttributes = new KeysAndAttributes().withKeys(Arrays.asList(key1, key2));
//        Map<String, KeysAndAttributes> requestItems = new HashMap<>();
//        requestItems.put(TABLE_NAME, keysAndAttributes);
//        Future<BatchGetItemResult> future = amazonDynamoDBAsync.batchGetItemAsync(
//                new BatchGetItemRequest(requestItems));
//
//        future.get();
//    }
//
//    @Trace(dispatcher = true)
//    private void batchPutTxnAsync() {
//        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
//
//        Map<String, AttributeValue> key1 = new HashMap<>();
//        Map<String, AttributeValue> key2 = new HashMap<>();
//
//        key1.put("artist", new AttributeValue("Thelonious Monk"));
//        key1.put("year", new AttributeValue().withN("1947"));
//
//        key2.put("artist", new AttributeValue("Sidney Bechet"));
//        key2.put("year", new AttributeValue().withN("1951"));
//
//        WriteRequest put1 = new WriteRequest(new PutRequest(key1));
//        WriteRequest put2 = new WriteRequest(new PutRequest(key2));
//
//        List<WriteRequest> writes = new ArrayList<>(Arrays.asList(put1, put2));
//        requestItems.put(TABLE_NAME, writes);
//
//        amazonDynamoDBAsync.batchWriteItemAsync(new BatchWriteItemRequest(requestItems),
//                new AsyncHandler<BatchWriteItemRequest, BatchWriteItemResult>() {
//                    @Override
//                    public void onError(Exception e) {
//                    }
//
//                    @Override
//                    public void onSuccess(BatchWriteItemRequest request, BatchWriteItemResult batchWriteItemResult) {
//                    }
//                });
//    }
//
//    @Test
//    public void testCreateTableAsync() throws InterruptedException, ExecutionException, TimeoutException {
//        createTableAsyncTxn();
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//        helper.assertScopedStatementMetricCount(txName, "createTable", ASYNC_TABLE_NAME, 1);
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Test
//    public void testDescribeTableAsync() {
//        describeTableTxnAsync();
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        assertEquals(1, introspector.getFinishedTransactionCount(10000));
//
//        String txName = introspector.getTransactionNames().iterator().next();
//        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
//        helper.assertScopedStatementMetricCount(txName, "describeTable", ASYNC_TABLE_NAME, 1);
//        helper.assertAggregateMetrics();
//        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
//    }
//
//    @Trace(dispatcher = true)
//    private void batchWriteTxn() {
//        TableWriteItems table1WriteItems = new TableWriteItems(TABLE_NAME).withItemsToPut(
//                new Item().withPrimaryKey("artist", "Charles Mingus", "year", 1959).withString("genre",
//                        "Jazz").withString("album", "Ah Um"));
//        TableWriteItems table2WriteItems = new TableWriteItems(SECOND_TABLE_NAME).withItemsToPut(
//                new Item().withPrimaryKey("artist", "Ornette Coleman", "year", 1959).withString("genre",
//                        "Free Jazz").withString("album", "Shape of Jazz to Come"));
//
//        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
//        dynamoDB.batchWriteItem(table1WriteItems, table2WriteItems);
//    }
//
// 
//
//    @Trace(dispatcher = true)
//    private void deleteTableTxn() {
//        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
//        dynamoDB.getTable(TABLE_NAME).delete();
//    }
//
//    @Trace(dispatcher = true)
//    private void queryTableTxn() {
//        Map<String, String> expressionAttributesNames = new HashMap<>();
//        expressionAttributesNames.put("#artist", "artist");
//
//        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
//        expressionAttributeValues.put(":artist", new AttributeValue().withS("Miles Davis"));
//
//        QueryRequest queryRequest = new QueryRequest().withTableName(TABLE_NAME).withKeyConditionExpression(
//                "#artist = :artist").withExpressionAttributeNames(
//                expressionAttributesNames).withExpressionAttributeValues(expressionAttributeValues);
//
//        amazonDynamoDB.query(queryRequest);
//    }
//
//    @Trace(dispatcher = true)
//    private void describeTableTxn() {
//        Table table = getOrCreateTable(TABLE_NAME);
//        table.describe();
//    }
//
//    @Trace(dispatcher = true)
//    private void putAndGetTxn() {
//        putItem();
//        getItem();
//    }
//
//    @Trace(dispatcher = true)
//    private void putAndGetTxnAsync() throws ExecutionException, InterruptedException {
//        putItemAsync();
//        getItemAsync();
//    }
//
//    @Trace(dispatcher = true)
//    private void getWrongItemAsyncTxn() {
//        getWrongItemAsync();
//        getRightItemAsync();
//    }
//
//    private void putItem() {
//        Table table = getOrCreateTable(TABLE_NAME);
//        Item item = DynamoUtil.createDefaultItem();
//        PutItemOutcome outcome = table.putItem(item);
//        outcome.getPutItemResult();
//    }
//
//    private void putItemAsync() throws ExecutionException, InterruptedException {
//        getOrCreateTable(TABLE_NAME);
//        Map<String, AttributeValue> key1 = new HashMap<>();
//        key1.put("artist", new AttributeValue("Eric Dolphy"));
//        key1.put("year", new AttributeValue().withN("1960"));
//        Future<PutItemResult> future = amazonDynamoDBAsync.putItemAsync(new PutItemRequest(TABLE_NAME, key1));
//        future.get();
//    }
//
//    private GetItemResult getItem() {
//        Map<String, AttributeValue> key = DynamoUtil.createItemKey();
//
//        GetItemRequest request = new GetItemRequest().withTableName(TABLE_NAME).withKey(key);
//
//        return amazonDynamoDB.getItem(request);
//    }
//
//    private void getItemAsync() throws ExecutionException, InterruptedException {
//        getOrCreateTable(TABLE_NAME);
//        Map<String, AttributeValue> key1 = new HashMap<>();
//        key1.put("artist", new AttributeValue("Charles Mingus"));
//        key1.put("year", new AttributeValue().withN("1959"));
//
//        Future<GetItemResult> future = amazonDynamoDBAsync.getItemAsync(new GetItemRequest(TABLE_NAME, key1));
//        future.get();
//    }
//
//    private void getWrongItemAsync() {
//        getOrCreateTable(TABLE_NAME);
//        Map<String, AttributeValue> key1 = new HashMap<>();
//        key1.put("shinger", new AttributeValue("Miley Cyrus"));
//        key1.put("year", new AttributeValue().withN("2300"));
//
//        amazonDynamoDBAsync.getItemAsync(new GetItemRequest(TABLE_NAME, key1),
//                new AsyncHandler<GetItemRequest, GetItemResult>() {
//                    @Override
//                    public void onError(Exception e) {
//                        NewRelic.addCustomParameter("Miley Cyrus is not jazz", 5);
//                    }
//
//                    @Override
//                    public void onSuccess(GetItemRequest request, GetItemResult getItemResult) {
//                        NewRelic.addCustomParameter("Miley Cyrus is totally jazz", 100);
//                    }
//                });
//    }
//
//    private void getRightItemAsync() {
//        getOrCreateTable(TABLE_NAME);
//        Map<String, AttributeValue> key1 = new HashMap<>();
//        key1.put("artist", new AttributeValue("Charles Mingus"));
//        key1.put("year", new AttributeValue().withN("1959"));
//
//        amazonDynamoDBAsync.getItemAsync(new GetItemRequest(TABLE_NAME, key1),
//                new AsyncHandler<GetItemRequest, GetItemResult>() {
//                    @Override
//                    public void onError(Exception e) {
//                        NewRelic.addCustomParameter("Mingus is not jazz", 5);
//                    }
//
//                    @Override
//                    public void onSuccess(GetItemRequest request, GetItemResult getItemResult) {
//                        NewRelic.addCustomParameter("Mingus is totally jazz", 100);
//                    }
//                });
//    }
//
//    @Trace(dispatcher = true)
//    private UpdateItemResult updateItemTxn() {
//        Map<String, AttributeValueUpdate> props = new HashMap<>();
//        props.put("rating", new AttributeValueUpdate().withValue(new AttributeValue().withS("5 stars")));
//
//        UpdateItemRequest request = new UpdateItemRequest().withTableName(TABLE_NAME).withKey(
//                DynamoUtil.createItemKey()).withAttributeUpdates(props);
//
//        return amazonDynamoDB.updateItem(request);
//    }
//
//    @Trace(dispatcher = true)
//    private void scanTableTxn() {
//        ScanRequest scanRequest = new ScanRequest().withTableName(TABLE_NAME);
//
//        amazonDynamoDB.scan(scanRequest);
//    }
//
//    @Trace(dispatcher = true)
//    private void scanTableTxnAsync() throws ExecutionException, InterruptedException {
//        ScanRequest scanRequest = new ScanRequest().withTableName(TABLE_NAME);
//        Future<ScanResult> future = amazonDynamoDBAsync.scanAsync(scanRequest);
//        future.get();
//    }
//
//    @Trace(dispatcher = true)
//    private void createTableAsyncTxn() throws InterruptedException, ExecutionException, TimeoutException {
//        final Future<CreateTableResult> future = getOrCreateTableAsync(ASYNC_TABLE_NAME);
//        future.get(5, TimeUnit.SECONDS);
//    }
//
//    @Trace(dispatcher = true)
//    private void describeTableTxnAsync() {
//        getOrCreateTable(ASYNC_TABLE_NAME);
//
//        final CountDownLatch countDownLatch = new CountDownLatch(1);
//        final DescribeTableResult[] result = new DescribeTableResult[1];
//
//        amazonDynamoDBAsync.describeTableAsync(ASYNC_TABLE_NAME,
//                new AsyncHandler<DescribeTableRequest, DescribeTableResult>() {
//                    @Override
//                    public void onError(Exception exception) {
//                        countDownLatch.countDown();
//                    }
//
//                    @Override
//                    public void onSuccess(DescribeTableRequest request, DescribeTableResult describeTableResult) {
//                        result[0] = describeTableResult;
//                        countDownLatch.countDown();
//                    }
//                });
//
//        try {
//            countDownLatch.await();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        result[0].getTable();
//    }
//
//    private Future<CreateTableResult> getOrCreateTableAsync(String tableName) {
//        return amazonDynamoDBAsync.createTableAsync(DynamoUtil.createTableRequest(tableName));
//    }

}
