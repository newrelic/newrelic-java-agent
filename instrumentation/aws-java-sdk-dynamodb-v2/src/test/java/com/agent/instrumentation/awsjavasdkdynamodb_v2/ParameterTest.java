package com.agent.instrumentation.awsjavasdkdynamodb_v2;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunnerWithParameters;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.runners.Parameterized.Parameters;
import static org.junit.runners.Parameterized.UseParametersRunnerFactory;

@RunWith(Parameterized.class)
@UseParametersRunnerFactory(InstrumentRunnerFactory.class)
@InstrumentationTestConfig(includePrefixes = {"software.amazon.awssdk.services.dynamodb", "com.nr.instrumentation"})
public class ParameterTest {
    private static final String SYNC = "sync";
    private static final String ASYNC = "async";
    private static final String DYNAMODB_PRODUCT = DatastoreVendor.DynamoDB.toString();

    @Parameters
    public static Iterable<String> data() throws Exception {
        return Arrays.asList(SYNC, ASYNC);
    }

    private String testMode;
    private static String hostName;
    private static DynamoDBProxyServer server;
    private static DynamoDbClient syncDynamoDbClient;
    private static DynamoDbAsyncClient asyncDynamoDbClient;
    private static String port;

    public ParameterTest(String testMode) throws Exception {
        this.testMode = testMode;
    }

    @Before
    public void beforeClass() throws Exception {
        port = String.valueOf(getRandomPort());
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

    @After
    public void afterClass() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testListAndCreateTable() throws ExecutionException, InterruptedException {
        System.out.println(testMode);
        //tests are run in random order. Must create table in order for metric to be generated.
        if (testMode.equals(SYNC)) {
            deleteTableTxn(testMode);
        } else {
            deleteTableAsyncTxn(testMode);
        }

        Introspector introspector = InstrumentationTestRunnerWithParameters.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(10000));
        introspector.clear();

        if (testMode.equals(SYNC)) {
            createTableTxn(testMode);
        } else {
            createTableAsyncTxn(testMode);
        }

        String txName = introspector.getTransactionNames().iterator().next();
        DatastoreHelper helper = new DatastoreHelper(DYNAMODB_PRODUCT);
        helper.assertScopedOperationMetricCount(txName, "listTables", 1);
        helper.assertScopedStatementMetricCount(txName, "createTable", testMode, 1);
        helper.assertInstanceLevelMetric(DYNAMODB_PRODUCT, hostName, port);
    }

    @Trace(dispatcher = true)
    private void deleteTableTxn(String testMode) {
        createTable(testMode);
        DeleteTableRequest request = DeleteTableRequest.builder().tableName(testMode).build();
        syncDynamoDbClient.deleteTable(request);
    }

    @Trace(dispatcher = true)
    private void createTableTxn(String testMode) {
        createTable(testMode);
    }

    private void createTable(String testMode) {
        if (tableExists(testMode)) {
            return;
        }
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(testMode)
                .keySchema(KeySchemaElement.builder().attributeName("artist").keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName("artist").attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(1000L).writeCapacityUnits(1000L)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();
        syncDynamoDbClient.createTable(request);
    }

    private boolean tableExists(String testMode) {
        ListTablesRequest request = ListTablesRequest.builder().build();
        ListTablesResponse listTableResponse = syncDynamoDbClient.listTables(request);
        return listTableResponse.tableNames().contains(testMode);
    }

    @Trace(dispatcher = true)
    private void deleteTableAsyncTxn(String testMode) throws ExecutionException, InterruptedException {
        createTableAsync(testMode);
        DeleteTableRequest request = DeleteTableRequest.builder().tableName(testMode).build();
        asyncDynamoDbClient.deleteTable(request).get();
    }

    @Trace(dispatcher = true)
    private void createTableAsyncTxn(String testMode) throws ExecutionException, InterruptedException {
        createTableAsync(testMode);
    }

    private void createTableAsync(String testMode) throws ExecutionException, InterruptedException {
        if (tableExistsAsync(testMode)) {
            return;
        }
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(testMode)
                .keySchema(KeySchemaElement.builder().attributeName("artist").keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName("artist").attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(1000L).writeCapacityUnits(1000L)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();
        asyncDynamoDbClient.createTable(request).get();
    }

    private boolean tableExistsAsync(String testMode) throws ExecutionException, InterruptedException {
        ListTablesRequest request = ListTablesRequest.builder().build();
        CompletableFuture<ListTablesResponse> listTableResponse = asyncDynamoDbClient.listTables(request);
        return listTableResponse.get().tableNames().contains(testMode);
    }


    private static int getRandomPort() {
        int port;
        try {
            ServerSocket socket = new ServerSocket(0);
            port = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to allocate ephemeral port");
        }
        return port;
    }
}

