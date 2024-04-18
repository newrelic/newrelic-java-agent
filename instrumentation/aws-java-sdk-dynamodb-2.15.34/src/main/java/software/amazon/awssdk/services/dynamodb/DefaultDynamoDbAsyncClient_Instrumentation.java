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
import java.util.concurrent.CompletableFuture;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.DefaultDynamoDbAsyncClient", type = MatchType.ExactClass)
final class DefaultDynamoDbAsyncClient_Instrumentation {

    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    @Trace(leaf = true)
    public CompletableFuture<ScanResponse> scan(ScanRequest scanRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scan", scanRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<PutItemResponse> putItem(PutItemRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "putItem", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<GetItemResponse> getItem(GetItemRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "getItem", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<DeleteItemResponse> deleteItem(DeleteItemRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteItem", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<ListTablesResponse> listTables(ListTablesRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTables", request.exclusiveStartTableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<DescribeTableResponse> describeTable(DescribeTableRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTable", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<CreateTableResponse> createTable(CreateTableRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createTable", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<DeleteTableResponse> deleteTable(DeleteTableRequest request) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteTable", request.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<BatchGetItemResponse> batchGetItem(BatchGetItemRequest batchGetItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchGetItem", "batch", clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<BatchWriteItemResponse> batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchWriteItem", "batch", clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<ListTagsOfResourceResponse> listTagsOfResource(ListTagsOfResourceRequest listTagsOfResourceRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTagsOfResource", listTagsOfResourceRequest.resourceArn(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<QueryResponse> query(QueryRequest queryRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "query", queryRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }


    @Trace(leaf = true)
    public CompletableFuture<UpdateItemResponse> updateItem(UpdateItemRequest updateItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateItem", updateItemRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<UpdateTableResponse> updateTable(UpdateTableRequest updateTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTable", updateTableRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<UpdateTimeToLiveResponse> updateTimeToLive(UpdateTimeToLiveRequest updateTimeToLiveRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTimeToLive", updateTimeToLiveRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<DescribeLimitsResponse> describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeLimits", null, clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<UntagResourceResponse> untagResource(UntagResourceRequest untagResourceRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "untagResource", untagResourceRequest.resourceArn(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<TagResourceResponse> tagResource(TagResourceRequest tagResourceRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "tagResource", tagResourceRequest.resourceArn(), clientConfiguration);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public CompletableFuture<DescribeTimeToLiveResponse> describeTimeToLive(DescribeTimeToLiveRequest describeTimeToLiveRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTimeToLive", describeTimeToLiveRequest.tableName(), clientConfiguration);
        return Weaver.callOriginal();
    }
 }