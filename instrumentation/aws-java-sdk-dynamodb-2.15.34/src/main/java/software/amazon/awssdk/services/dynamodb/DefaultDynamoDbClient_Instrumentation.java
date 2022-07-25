package software.amazon.awssdk.services.dynamodb;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.dynamodb_v2.DynamoDBMetricUtil;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.DefaultDynamoDbClient", type = MatchType.ExactClass)
final class DefaultDynamoDbClient_Instrumentation {
    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    @Trace
    public GetItemResponse getItem(GetItemRequest getItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "getItem", getItemRequest.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public ListTagsOfResourceResponse listTagsOfResource(ListTagsOfResourceRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTagsOfResource", null, getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public UntagResourceResponse untagResource(UntagResourceRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "untagResource", null, getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public BatchGetItemResponse batchGetItem(BatchGetItemRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchGetItem", "batch", getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public BatchWriteItemResponse batchWriteItem(BatchWriteItemRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchWriteItem", "batch", getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public PutItemResponse putItem(PutItemRequest putItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "putItem", putItemRequest.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteItemResponse deleteItem(DeleteItemRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteItem", request.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public ListTablesResponse listTables(ListTablesRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTables", request.exclusiveStartTableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public CreateTableResponse createTable(CreateTableRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createTable", request.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteTableResponse deleteTable(DeleteTableRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteTable", request.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeTableResponse describeTable(DescribeTableRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTable", request.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public ScanResponse scan(ScanRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scan", request.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse query(QueryRequest queryRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "query", queryRequest.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateItemResponse updateItem(UpdateItemRequest updateItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateItem", updateItemRequest.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateTableResponse updateTable(UpdateTableRequest updateTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTable", updateTableRequest.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateTimeToLiveResponse updateTimeToLive(UpdateTimeToLiveRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTimeToLive", request.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeLimitsResponse describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeLimits", null, getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public TagResourceResponse tagResource(TagResourceRequest tagResourceRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "tagResource", tagResourceRequest.resourceArn(), getEndpoint());
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeTimeToLiveResponse describeTimeToLive(DescribeTimeToLiveRequest describeTimeToLiveRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTimeToLive", describeTimeToLiveRequest.tableName(), getEndpoint());
        return Weaver.callOriginal();
    }

    private URI getEndpoint() {
        return clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
    }
}