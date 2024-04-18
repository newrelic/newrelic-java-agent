package com.agent.instrumentation.awsjavasdkdynamodb_v2;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.transport.apache.ApacheHttpClientWrapper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LocalTestDynamoDb {
    private static final String TABLE_NAME = "test";

    private final String hostName;
    private final String port;
    private final DynamoDBProxyServer server;
    private final DynamoDbClient client;
    private final DynamoDbAsyncClient asyncClient;

    private LocalTestDynamoDb() throws Exception {
        port = String.valueOf(InstrumentationTestRunner.getIntrospector().getRandomPort());
        hostName = InetAddress.getLocalHost().getHostName();
        server = ServerRunner.createServerFromCommandLineArgs(new String[]{"-disableTelemetry", "-inMemory", "-port", port});
        AwsCredentials awsCredentials = AwsBasicCredentials.create("1234QAAAAAAAZZZZZZZZ", "secret");
        client = DynamoDbClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .endpointOverride(new URI("http://localhost:" + port))
                .region(Region.US_WEST_1)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        asyncClient = DynamoDbAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .endpointOverride(new URI("http://localhost:" + port))
                .region(Region.US_WEST_1).build();
    }

    public static LocalTestDynamoDb create() throws Exception {
        return new LocalTestDynamoDb();
    }

    private static void tryToGetCompletableFuture(CompletableFuture<?> completableFuture) {
        try {
            completableFuture.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getHostName() {
        return hostName;
    }

    public String getPort() {
        return port;
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public boolean tableExists() {
        ListTablesRequest request = ListTablesRequest.builder().build();
        ListTablesResponse listTableResponse = client.listTables(request);
        return listTableResponse.tableNames().contains(TABLE_NAME);
    }


    // AWS service call simulations
    // NOTE: We cannot simulate 'tag' methods for DynamoDb API test purpose
    //       because tags are not supported by the DynamoDBLocal.
    //       See DefaultDynamoDbClient_InstrumentationTagMethodsTest for instrumentation validation on these methods.

    public void describeTimeToLive() {
        client.describeTimeToLive(describeTimeToLiveRequest());
    }

    public void describeTimeToLiveAsync() {
        tryToGetCompletableFuture(asyncClient.describeTimeToLive(describeTimeToLiveRequest()));
    }

    private DescribeTimeToLiveRequest describeTimeToLiveRequest() {
        return DescribeTimeToLiveRequest.builder().tableName(TABLE_NAME).build();
    }

    public void describeLimits() {
        client.describeLimits(DescribeLimitsRequest.builder().build());
    }

    public void describeLimitsAsync() {
        tryToGetCompletableFuture(asyncClient.describeLimits(DescribeLimitsRequest.builder().build()));
    }

    public void updateTable() {
        client.updateTable(updateTableRequest());
    }

    public void updateTableAsync() {
        tryToGetCompletableFuture(asyncClient.updateTable(updateTableRequest()));
    }

    private UpdateTableRequest updateTableRequest() {
        return UpdateTableRequest.builder()
                .tableName(TABLE_NAME)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();
    }

    public void updateTimeToLive() {
        client.updateTimeToLive(updateTimeToLiveRequest());
    }

    public void updateTimeToLiveAsync() {
        tryToGetCompletableFuture(asyncClient.updateTimeToLive(updateTimeToLiveRequest()));
    }

    private UpdateTimeToLiveRequest updateTimeToLiveRequest() {
        TimeToLiveSpecification spec = TimeToLiveSpecification.builder()
                .attributeName("ttl")
                .enabled(true)
                .build();
        return UpdateTimeToLiveRequest.builder()
                .tableName(TABLE_NAME)
                .timeToLiveSpecification(spec)
                .build();
    }

    public void batchWriteItem() {
        client.batchWriteItem(batchWriteItemRequest());
    }

    public void batchWriteItemAsync() {
        tryToGetCompletableFuture(asyncClient.batchWriteItem(batchWriteItemRequest()));
    }

    public BatchWriteItemRequest batchWriteItemRequest() {
        Map<String, Collection<WriteRequest>> itemValues = new HashMap<>();
        PutRequest putRequest = PutRequest.builder()
                .item(defaultItem()).build();
        WriteRequest writeRequest = WriteRequest.builder()
                .putRequest(putRequest).build();
        itemValues.put(TABLE_NAME, Collections.singletonList(writeRequest));
        return BatchWriteItemRequest.builder()
                .requestItems(itemValues)
                .build();
    }

    public void batchGetItem() {
        client.batchGetItem(batchGetItemRequest());
    }

    public void batchGetItemAsync() {
        tryToGetCompletableFuture(asyncClient.batchGetItem(batchGetItemRequest()));
    }

    private BatchGetItemRequest batchGetItemRequest() {
        Map<String, AttributeValue> key1 = new HashMap<>();
        key1.put("artist", AttributeValue.builder().s("Pink").build());
        KeysAndAttributes keysAndAttributes =
                KeysAndAttributes.builder().keys(Collections.singletonList(key1)).build();
        Map<String, KeysAndAttributes> requestItems = new HashMap<>();
        requestItems.put(TABLE_NAME, keysAndAttributes);

        return BatchGetItemRequest.builder()
                .requestItems(requestItems)
                .build();
    }

    public void getItem() {
        client.getItem(getItemRequest());
    }

    public void getItemAsync() {
        tryToGetCompletableFuture(asyncClient.getItem(getItemRequest()));
    }

    private GetItemRequest getItemRequest() {
        return GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Collections.singletonMap("artist", AttributeValue.builder().s("Pink").build()))
                .build();
    }

    public void putItem() {
        client.putItem(putItemRequest());
    }

    public void putItemAsync() {
        tryToGetCompletableFuture(asyncClient.putItem(putItemRequest()));
    }

    private PutItemRequest putItemRequest() {
        return PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(defaultItem())
                .build();
    }

    public void updateItem() {
        client.updateItem(updateItemRequest());
    }

    public void updateItemAsync() {
        tryToGetCompletableFuture(asyncClient.updateItem(updateItemRequest()));
    }

    private UpdateItemRequest updateItemRequest() {
        Map<String, AttributeValue> itemKey = new HashMap<>();

        itemKey.put("artist", AttributeValue.builder().s("Pink").build());

        Map<String, AttributeValueUpdate> updatedValues = new HashMap<>();

        updatedValues.put("rating", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s("5 stars").build())
                .action(AttributeAction.PUT)
                .build());

        return UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(itemKey)
                .attributeUpdates(updatedValues)
                .build();
    }

    public void deleteItem() {
        client.deleteItem(deleteItemRequest());
    }

    public void deleteItemAsync() {
        tryToGetCompletableFuture(asyncClient.deleteItem(deleteItemRequest()));
    }

    private DeleteItemRequest deleteItemRequest() {
        return DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Collections.singletonMap("artist", AttributeValue.builder().s("Pink").build()))
                .build();
    }

    public Map<String, AttributeValue> defaultItem() {
        Map<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("artist", AttributeValue.builder().s("Pink").build());
        itemValues.put("songTitle", AttributeValue.builder().s("lazy river").build());
        return itemValues;
    }

    public void listTables() {
        client.listTables(ListTablesRequest.builder().build());
    }

    public void query() {
        client.query(queryRequest());
    }

    public void queryAsync() {
        tryToGetCompletableFuture(asyncClient.query(queryRequest()));
    }

    private QueryRequest queryRequest() {
        Map<String, String> attrNameAlias = new HashMap<>();
        attrNameAlias.put("#artist", "artist");

        Map<String, AttributeValue> attrValues =
                new HashMap<>();

        attrValues.put(":artist", AttributeValue.builder()
                .s("Miles Davis")
                .build());

        return QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("#artist = :artist")
                .expressionAttributeNames(attrNameAlias)
                .expressionAttributeValues(attrValues)
                .build();
    }

    public void scan() {
        client.scan(ScanRequest.builder().tableName(TABLE_NAME).build());
    }

    public void scanAsync() {
        tryToGetCompletableFuture(asyncClient.scan(ScanRequest.builder().tableName(TABLE_NAME).build()));
    }

    public void deleteTable() {
        client.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());

    }

    public void describeTable() {
        client.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build());
    }

    public void describeTableAsync() {
        tryToGetCompletableFuture(
                asyncClient.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build()));
    }

    private CreateTableRequest createTableRequest() {
        return CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder().attributeName("artist").keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName("artist").attributeType(ScalarAttributeType.S).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();
    }

    public void createTable() {
        client.createTable(createTableRequest());
    }

    public void createTableAsync() {
        tryToGetCompletableFuture(asyncClient.createTable(createTableRequest()));
    }
}
