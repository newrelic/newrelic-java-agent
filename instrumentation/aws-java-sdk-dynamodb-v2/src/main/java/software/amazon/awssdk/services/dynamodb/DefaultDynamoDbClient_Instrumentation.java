package software.amazon.awssdk.services.dynamodb;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.dynamodb_v2.DynamoDBMetricUtil;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.net.URI;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.DefaultDynamoDbClient", type = MatchType.ExactClass)
final class DefaultDynamoDbClient_Instrumentation {
    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    @Trace
    public GetItemResponse getItem(GetItemRequest getItemRequest) {
        URI endpoint = clientConfiguration.option(SdkClientOption.ENDPOINT);
        System.out.println("getItem -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "getItem", getItemRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListTablesResponse listTables(ListTablesRequest request) {
        URI endpoint = clientConfiguration.option(SdkClientOption.ENDPOINT);
        System.out.println("listTables -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTables", request.exclusiveStartTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CreateTableResponse createTable(CreateTableRequest request) {
        URI endpoint = clientConfiguration.option(SdkClientOption.ENDPOINT);
        System.out.println("createTable -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createTable", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteTableResponse deleteTable(DeleteTableRequest request) {
        URI endpoint = clientConfiguration.option(SdkClientOption.ENDPOINT);
        System.out.println("deleteTable -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteTable", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeTableResponse describeTable(DescribeTableRequest request) {
        URI endpoint = clientConfiguration.option(SdkClientOption.ENDPOINT);
        System.out.println("describeTable -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTable", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ScanResponse scan(ScanRequest request) {
        URI endpoint = clientConfiguration.option(SdkClientOption.ENDPOINT);
        System.out.println("scan -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scan", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }
}