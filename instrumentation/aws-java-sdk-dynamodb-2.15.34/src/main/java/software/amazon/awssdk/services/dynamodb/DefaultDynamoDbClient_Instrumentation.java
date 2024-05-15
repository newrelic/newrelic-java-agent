package software.amazon.awssdk.services.dynamodb;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.dynamodb_v2.DynamoDBMetricUtil;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.DefaultDynamoDbClient", type = MatchType.ExactClass)
final class DefaultDynamoDbClient_Instrumentation {
    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    @Trace(leaf = true)
    public GetItemResponse getItem(GetItemRequest getItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "getItem", getItemRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public ListTagsOfResourceResponse listTagsOfResource(ListTagsOfResourceRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTagsOfResource", null, clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public UntagResourceResponse untagResource(UntagResourceRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "untagResource", null, clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public BatchGetItemResponse batchGetItem(BatchGetItemRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchGetItem", "batch", clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public BatchWriteItemResponse batchWriteItem(BatchWriteItemRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchWriteItem", "batch", clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public PutItemResponse putItem(PutItemRequest putItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "putItem", putItemRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public DeleteItemResponse deleteItem(DeleteItemRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteItem", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public ListTablesResponse listTables(ListTablesRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTables", request.exclusiveStartTableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CreateTableResponse createTable(CreateTableRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createTable", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public DeleteTableResponse deleteTable(DeleteTableRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteTable", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public DescribeTableResponse describeTable(DescribeTableRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTable", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public ScanResponse scan(ScanRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scan", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public QueryResponse query(QueryRequest queryRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "query", queryRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public UpdateItemResponse updateItem(UpdateItemRequest updateItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateItem", updateItemRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public UpdateTableResponse updateTable(UpdateTableRequest updateTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTable", updateTableRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public UpdateTimeToLiveResponse updateTimeToLive(UpdateTimeToLiveRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTimeToLive", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public DescribeLimitsResponse describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeLimits", null, clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public TagResourceResponse tagResource(TagResourceRequest tagResourceRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "tagResource", tagResourceRequest.resourceArn(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public DescribeTimeToLiveResponse describeTimeToLive(DescribeTimeToLiveRequest describeTimeToLiveRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTimeToLive", describeTimeToLiveRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }
}