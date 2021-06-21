package software.amazon.awssdk.services.dynamodb;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.dynamodb_v2.DynamoDBMetricUtil;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.dynamodb.model.BatchExecuteStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchExecuteStatementResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.CreateBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateBackupResponse;
import software.amazon.awssdk.services.dynamodb.model.CreateGlobalTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateGlobalTableResponse;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteBackupResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeBackupResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeContinuousBackupsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeContinuousBackupsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeContributorInsightsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeContributorInsightsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeEndpointsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeEndpointsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeExportRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeExportResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeGlobalTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeGlobalTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeGlobalTableSettingsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeGlobalTableSettingsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeKinesisStreamingDestinationRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeKinesisStreamingDestinationResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeLimitsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeLimitsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableReplicaAutoScalingRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableReplicaAutoScalingResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveResponse;
import software.amazon.awssdk.services.dynamodb.model.DisableKinesisStreamingDestinationRequest;
import software.amazon.awssdk.services.dynamodb.model.DisableKinesisStreamingDestinationResponse;
import software.amazon.awssdk.services.dynamodb.model.EnableKinesisStreamingDestinationRequest;
import software.amazon.awssdk.services.dynamodb.model.EnableKinesisStreamingDestinationResponse;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementResponse;
import software.amazon.awssdk.services.dynamodb.model.ExecuteTransactionRequest;
import software.amazon.awssdk.services.dynamodb.model.ExecuteTransactionResponse;
import software.amazon.awssdk.services.dynamodb.model.ExportTableToPointInTimeRequest;
import software.amazon.awssdk.services.dynamodb.model.ExportTableToPointInTimeResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ListBackupsRequest;
import software.amazon.awssdk.services.dynamodb.model.ListBackupsResponse;
import software.amazon.awssdk.services.dynamodb.model.ListContributorInsightsRequest;
import software.amazon.awssdk.services.dynamodb.model.ListContributorInsightsResponse;
import software.amazon.awssdk.services.dynamodb.model.ListExportsRequest;
import software.amazon.awssdk.services.dynamodb.model.ListExportsResponse;
import software.amazon.awssdk.services.dynamodb.model.ListGlobalTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListGlobalTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTagsOfResourceRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTagsOfResourceResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.RestoreTableFromBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.RestoreTableFromBackupResponse;
import software.amazon.awssdk.services.dynamodb.model.RestoreTableToPointInTimeRequest;
import software.amazon.awssdk.services.dynamodb.model.RestoreTableToPointInTimeResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TagResourceRequest;
import software.amazon.awssdk.services.dynamodb.model.TagResourceResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;
import software.amazon.awssdk.services.dynamodb.model.UntagResourceRequest;
import software.amazon.awssdk.services.dynamodb.model.UntagResourceResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateContinuousBackupsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateContinuousBackupsResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateContributorInsightsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateContributorInsightsResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableSettingsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableSettingsResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableReplicaAutoScalingRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableReplicaAutoScalingResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveResponse;
import software.amazon.awssdk.services.dynamodb.paginators.BatchGetItemIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ListContributorInsightsIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ListExportsIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ListTablesIterable;
import software.amazon.awssdk.services.dynamodb.paginators.QueryIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.net.URI;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.DefaultDynamoDbClient", type = MatchType.ExactClass)
final class DefaultDynamoDbClient_Instrumentation {
    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();
    private final URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;

    // TODO: Batch operations need a 'collection' parameter since 'table()' is unavailable on the requests
    @Trace
    public BatchExecuteStatementResponse batchExecuteStatement(BatchExecuteStatementRequest batchExecuteStatementRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchExecuteStatement", "batch", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public BatchGetItemResponse batchGetItem(BatchGetItemRequest batchGetItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchGetItem", "batch", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public BatchGetItemIterable batchGetItemPaginator(BatchGetItemRequest batchGetItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchGetItemPaginator", "batch", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public BatchWriteItemResponse batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchWriteItem", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CreateBackupResponse createBackup(CreateBackupRequest createBackupRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createBackup", createBackupRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CreateGlobalTableResponse createGlobalTable(CreateGlobalTableRequest createGlobalTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createGlobalTable", createGlobalTableRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public GetItemResponse getItem(GetItemRequest getItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "getItem", getItemRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CreateTableResponse createTable(CreateTableRequest createTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createTable", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteBackupResponse deleteBackup(DeleteBackupRequest deleteBackupRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteBackup", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteItemResponse deleteItem(DeleteItemRequest deleteItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteItem", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteTableResponse deleteTable(DeleteTableRequest deleteTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteTable", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeBackupResponse describeBackup(DescribeBackupRequest describeBackupRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeBackup", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeContinuousBackupsResponse describeContinuousBackups(DescribeContinuousBackupsRequest describeContinuousBackupsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeContinuousBackups", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeContributorInsightsResponse describeContributorInsights(DescribeContributorInsightsRequest describeContributorInsightsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeContributorInsights", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeEndpointsResponse describeEndpoints(DescribeEndpointsRequest describeEndpointsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeEndpoints", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeExportResponse describeExport(DescribeExportRequest describeExportRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeExport", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeGlobalTableResponse describeGlobalTable(DescribeGlobalTableRequest describeGlobalTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeGlobalTable", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeGlobalTableSettingsResponse describeGlobalTableSettings(DescribeGlobalTableSettingsRequest describeGlobalTableSettingsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeGlobalTableSettings", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeKinesisStreamingDestinationResponse describeKinesisStreamingDestination(
            DescribeKinesisStreamingDestinationRequest describeKinesisStreamingDestinationRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeKinesisStreamingDestination", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeLimitsResponse describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeLimits", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeTableResponse describeTable(DescribeTableRequest describeTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTable", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeTableReplicaAutoScalingResponse describeTableReplicaAutoScaling(
            DescribeTableReplicaAutoScalingRequest describeTableReplicaAutoScalingRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTableReplicaAutoScaling", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeTimeToLiveResponse describeTimeToLive(DescribeTimeToLiveRequest describeTimeToLiveRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTimeToLive", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DisableKinesisStreamingDestinationResponse disableKinesisStreamingDestination(
            DisableKinesisStreamingDestinationRequest disableKinesisStreamingDestinationRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "disableKinesisStreamingDestination", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public EnableKinesisStreamingDestinationResponse enableKinesisStreamingDestination(
            EnableKinesisStreamingDestinationRequest enableKinesisStreamingDestinationRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "enableKinesisStreamingDestination", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ExecuteStatementResponse executeStatement(ExecuteStatementRequest executeStatementRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "executeStatement", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ExecuteTransactionResponse executeTransaction(ExecuteTransactionRequest executeTransactionRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "executeTransaction", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ExportTableToPointInTimeResponse exportTableToPointInTime(ExportTableToPointInTimeRequest exportTableToPointInTimeRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "exportTableToPointInTime", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListBackupsResponse listBackups(ListBackupsRequest listBackupsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listBackups", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListContributorInsightsResponse listContributorInsights(ListContributorInsightsRequest listContributorInsightsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listContributorInsights", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListContributorInsightsIterable listContributorInsightsPaginator(ListContributorInsightsRequest listContributorInsightsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listContributorInsightsPaginator", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListExportsResponse listExports(ListExportsRequest listExportsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listExports", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListExportsIterable listExportsPaginator(ListExportsRequest listExportsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listExportsPaginator", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListGlobalTablesResponse listGlobalTables(ListGlobalTablesRequest listGlobalTablesRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listGlobalTables", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListTablesResponse listTables(ListTablesRequest listTablesRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTables", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListTablesIterable listTablesPaginator(ListTablesRequest listTablesRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTablesPaginator", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListTagsOfResourceResponse listTagsOfResource(ListTagsOfResourceRequest listTagsOfResourceRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTagsOfResource", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public PutItemResponse putItem(PutItemRequest putItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "putItem", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse query(QueryRequest queryRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "query", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryIterable queryPaginator(QueryRequest queryRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "queryPaginator", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public RestoreTableFromBackupResponse restoreTableFromBackup(RestoreTableFromBackupRequest restoreTableFromBackupRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "restoreTableFromBackup", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public RestoreTableToPointInTimeResponse restoreTableToPointInTime(RestoreTableToPointInTimeRequest restoreTableToPointInTimeRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "restoreTableToPointInTime", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ScanResponse scan(ScanRequest scanRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scan", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ScanIterable scanPaginator(ScanRequest scanRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scanPaginator", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public TagResourceResponse tagResource(TagResourceRequest tagResourceRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "tagResource", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public TransactGetItemsResponse transactGetItems(TransactGetItemsRequest transactGetItemsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "transactGetItems", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public TransactWriteItemsResponse transactWriteItems(TransactWriteItemsRequest transactWriteItemsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "transactWriteItems", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UntagResourceResponse untagResource(UntagResourceRequest untagResourceRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "untagResource", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateContinuousBackupsResponse updateContinuousBackups(UpdateContinuousBackupsRequest updateContinuousBackupsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateContinuousBackups", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateContributorInsightsResponse updateContributorInsights(UpdateContributorInsightsRequest updateContributorInsightsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateContributorInsights", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateGlobalTableResponse updateGlobalTable(UpdateGlobalTableRequest updateGlobalTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateGlobalTable", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateGlobalTableSettingsResponse updateGlobalTableSettings(UpdateGlobalTableSettingsRequest updateGlobalTableSettingsRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateGlobalTableSettings", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateItemResponse updateItem(UpdateItemRequest updateItemRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateItem", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateTableResponse updateTable(UpdateTableRequest updateTableRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTable", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateTableReplicaAutoScalingResponse updateTableReplicaAutoScaling(UpdateTableReplicaAutoScalingRequest updateTableReplicaAutoScalingRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTableReplicaAutoScaling", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateTimeToLiveResponse updateTimeToLive(UpdateTimeToLiveRequest updateTimeToLiveRequest) {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTimeToLive", "????", endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public void close() {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "close", "????", endpoint);
        Weaver.callOriginal();
    }

    @Trace
    public DynamoDbWaiter waiter() {
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "waiter", "????", endpoint);
        return Weaver.callOriginal();
    }
}
