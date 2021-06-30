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
import software.amazon.awssdk.services.dynamodb.paginators.*;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.DefaultDynamoDbAsyncClient", type = MatchType.ExactClass)
final class DefaultDynamoDbAsyncClient_Instrumentation {

    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    @Trace
    public CompletableFuture<ScanResponse> scan(ScanRequest scanRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        System.out.println("-> scan async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scan", scanRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<PutItemResponse> putItem(PutItemRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        System.out.println("-> putItem async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "putItem", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<GetItemResponse> getItem(GetItemRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        System.out.println("-> getItem async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "getItem", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DeleteItemResponse> deleteItem(DeleteItemRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        System.out.println("-> deleteItem async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteItem", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<ListTablesResponse> listTables(ListTablesRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        System.out.println("-> listTables async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTables", request.exclusiveStartTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DescribeTableResponse> describeTable(DescribeTableRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        System.out.println("-> describeTable async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTable", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<CreateTableResponse> createTable(CreateTableRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        System.out.println("-> createTable async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createTable", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DeleteTableResponse> deleteTable(DeleteTableRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        System.out.println("-> deleteTable async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteTable", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    //generated 

    @Trace
    public CompletableFuture<BatchExecuteStatementResponse> batchExecuteStatement(BatchExecuteStatementRequest batchExecuteStatementRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchExecuteStatement", "batch", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<BatchGetItemResponse> batchGetItem(BatchGetItemRequest batchGetItemRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchGetItem", "batch", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public BatchGetItemPublisher batchGetItemPaginator(BatchGetItemRequest batchGetItemRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchGetItemPaginator", "batch", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<BatchWriteItemResponse> batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchWriteItem", "batch", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<CreateBackupResponse> createBackup(CreateBackupRequest createBackupRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createBackup", createBackupRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<CreateGlobalTableResponse> createGlobalTable(CreateGlobalTableRequest createGlobalTableRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createGlobalTable", createGlobalTableRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }
//
//    @Trace
//    public CompletableFuture<DeleteBackupResponse> deleteBackup(DeleteBackupRequest deleteBackupRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteBackup", deleteBackupRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

//    @Trace
//    public CompletableFuture<DescribeBackupResponse> describeBackup(DescribeBackupRequest describeBackupRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeBackup", describeBackupRequest.tablename(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public CompletableFuture<DescribeContinuousBackupsResponse> describeContinuousBackups(DescribeContinuousBackupsRequest describeContinuousBackupsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeContinuousBackups", describeContinuousBackupsRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DescribeContributorInsightsResponse> describeContributorInsights(DescribeContributorInsightsRequest describeContributorInsightsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeContributorInsights", describeContributorInsightsRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

//    @Trace
//    public CompletableFuture<DescribeEndpointsResponse> describeEndpoints(DescribeEndpointsRequest describeEndpointsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeEndpoints", describeEndpointsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

//    @Trace
//    public CompletableFuture<DescribeExportResponse> describeExport(DescribeExportRequest describeExportRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeExport", describeExportRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public CompletableFuture<DescribeGlobalTableResponse> describeGlobalTable(DescribeGlobalTableRequest describeGlobalTableRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeGlobalTable", describeGlobalTableRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DescribeGlobalTableSettingsResponse> describeGlobalTableSettings(DescribeGlobalTableSettingsRequest describeGlobalTableSettingsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeGlobalTableSettings", describeGlobalTableSettingsRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DescribeKinesisStreamingDestinationResponse> describeKinesisStreamingDestination(DescribeKinesisStreamingDestinationRequest describeKinesisStreamingDestinationRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeKinesisStreamingDestination", describeKinesisStreamingDestinationRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DescribeLimitsResponse> describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeLimits", null, endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DescribeTableReplicaAutoScalingResponse> describeTableReplicaAutoScaling(DescribeTableReplicaAutoScalingRequest describeTableReplicaAutoScalingRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTableReplicaAutoScaling", describeTableReplicaAutoScalingRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DescribeTimeToLiveResponse> describeTimeToLive(DescribeTimeToLiveRequest describeTimeToLiveRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTimeToLive", describeTimeToLiveRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DisableKinesisStreamingDestinationResponse> disableKinesisStreamingDestination(DisableKinesisStreamingDestinationRequest disableKinesisStreamingDestinationRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "disableKinesisStreamingDestination", disableKinesisStreamingDestinationRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<EnableKinesisStreamingDestinationResponse> enableKinesisStreamingDestination(EnableKinesisStreamingDestinationRequest enableKinesisStreamingDestinationRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "enableKinesisStreamingDestination", enableKinesisStreamingDestinationRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<ExecuteStatementResponse> executeStatement(ExecuteStatementRequest executeStatementRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "executeStatement", null, endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<ExecuteTransactionResponse> executeTransaction(ExecuteTransactionRequest executeTransactionRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "executeTransaction", null, endpoint);
        return Weaver.callOriginal();
    }

//    @Trace
//    public CompletableFuture<ExportTableToPointInTimeResponse> exportTableToPointInTime(ExportTableToPointInTimeRequest exportTableToPointInTimeRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "exportTableToPointInTime", exportTableToPointInTimeRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public CompletableFuture<ListBackupsResponse> listBackups(ListBackupsRequest listBackupsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listBackups", listBackupsRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<ListContributorInsightsResponse> listContributorInsights(ListContributorInsightsRequest listContributorInsightsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listContributorInsights", listContributorInsightsRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListContributorInsightsPublisher listContributorInsightsPaginator(ListContributorInsightsRequest listContributorInsightsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listContributorInsightsPaginator", listContributorInsightsRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

//    @Trace
//    public CompletableFuture<ListExportsResponse> listExports(ListExportsRequest listExportsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listExports", listExportsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

//    @Trace
//    public ListExportsPublisher listExportsPaginator(ListExportsRequest listExportsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listExportsPaginator", listExportsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public CompletableFuture<ListGlobalTablesResponse> listGlobalTables(ListGlobalTablesRequest listGlobalTablesRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listGlobalTables", listGlobalTablesRequest.exclusiveStartGlobalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListTablesPublisher listTablesPaginator(ListTablesRequest listTablesRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTablesPaginator", listTablesRequest.exclusiveStartTableName(), endpoint);
        return Weaver.callOriginal();
    }

//    @Trace
//    public CompletableFuture<ListTagsOfResourceResponse> listTagsOfResource(ListTagsOfResourceRequest listTagsOfResourceRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTagsOfResource", listTagsOfResourceRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public CompletableFuture<QueryResponse> query(QueryRequest queryRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "query", queryRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryPublisher queryPaginator(QueryRequest queryRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "queryPaginator", queryRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<RestoreTableFromBackupResponse> restoreTableFromBackup(RestoreTableFromBackupRequest restoreTableFromBackupRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "restoreTableFromBackup", restoreTableFromBackupRequest.targetTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<RestoreTableToPointInTimeResponse> restoreTableToPointInTime(RestoreTableToPointInTimeRequest restoreTableToPointInTimeRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "restoreTableToPointInTime", restoreTableToPointInTimeRequest.targetTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ScanPublisher scanPaginator(ScanRequest scanRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scanPaginator", scanRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

//    @Trace
//    public CompletableFuture<TagResourceResponse> tagResource(TagResourceRequest tagResourceRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "tagResource", tagResourceRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

//    @Trace
//    public CompletableFuture<TransactGetItemsResponse> transactGetItems(TransactGetItemsRequest transactGetItemsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "transactGetItems", transactGetItemsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
//    @Trace
//    public CompletableFuture<TransactWriteItemsResponse> transactWriteItems(TransactWriteItemsRequest transactWriteItemsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "transactWriteItems", transactWriteItemsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
//    @Trace
//    public CompletableFuture<UntagResourceResponse> untagResource(UntagResourceRequest untagResourceRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "untagResource", untagResourceRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public CompletableFuture<UpdateContinuousBackupsResponse> updateContinuousBackups(UpdateContinuousBackupsRequest updateContinuousBackupsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateContinuousBackups", updateContinuousBackupsRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<UpdateContributorInsightsResponse> updateContributorInsights(UpdateContributorInsightsRequest updateContributorInsightsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateContributorInsights", updateContributorInsightsRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<UpdateGlobalTableResponse> updateGlobalTable(UpdateGlobalTableRequest updateGlobalTableRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateGlobalTable", updateGlobalTableRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<UpdateGlobalTableSettingsResponse> updateGlobalTableSettings(UpdateGlobalTableSettingsRequest updateGlobalTableSettingsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateGlobalTableSettings", updateGlobalTableSettingsRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<UpdateItemResponse> updateItem(UpdateItemRequest updateItemRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateItem", updateItemRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<UpdateTableResponse> updateTable(UpdateTableRequest updateTableRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTable", updateTableRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<UpdateTableReplicaAutoScalingResponse> updateTableReplicaAutoScaling(UpdateTableReplicaAutoScalingRequest updateTableReplicaAutoScalingRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTableReplicaAutoScaling", updateTableReplicaAutoScalingRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<UpdateTimeToLiveResponse> updateTimeToLive(UpdateTimeToLiveRequest updateTimeToLiveRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTimeToLive", updateTimeToLiveRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }
}