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
import software.amazon.awssdk.services.dynamodb.paginators.ListTablesIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;

import java.net.URI;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.DefaultDynamoDbClient", type = MatchType.ExactClass)
final class DefaultDynamoDbClient_Instrumentation {
    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    @Trace
    public GetItemResponse getItem(GetItemRequest getItemRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        System.out.println("getItem -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "getItem", getItemRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public PutItemResponse putItem(PutItemRequest putItemRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        System.out.println("putItem -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "putItem", putItemRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteItemResponse deleteItem(DeleteItemRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteItem", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListTablesResponse listTables(ListTablesRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        System.out.println("listTables -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTables", request.exclusiveStartTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CreateTableResponse createTable(CreateTableRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        System.out.println("createTable -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createTable", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteTableResponse deleteTable(DeleteTableRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        System.out.println("deleteTable -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteTable", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeTableResponse describeTable(DescribeTableRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        System.out.println("describeTable -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTable", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ScanResponse scan(ScanRequest request) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        System.out.println("scan -> sync client endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scan", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    //generated

//    @Trace
//    public DeleteBackupResponse deleteBackup(DeleteBackupRequest deleteBackupRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteBackup", deleteBackupRequest., endpoint);
//        return Weaver.callOriginal();
//    }

//    @Trace
//    public DescribeBackupResponse describeBackup(DescribeBackupRequest describeBackupRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeBackup", describeBackupRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
//    @Trace
//    public DescribeContinuousBackupsResponse describeContinuousBackups(DescribeContinuousBackupsRequest describeContinuousBackupsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeContinuousBackups", describeContinuousBackupsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
//    @Trace
//    public DescribeContributorInsightsResponse describeContributorInsights(DescribeContributorInsightsRequest describeContributorInsightsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeContributorInsights", describeContributorInsightsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
//    @Trace
//    public DescribeEndpointsResponse describeEndpoints(DescribeEndpointsRequest describeEndpointsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeEndpoints", describeEndpointsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    //    @Trace
//    public DescribeExportResponse describeExport(DescribeExportRequest describeExportRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeExport", describeExportRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
    @Trace
    public DescribeGlobalTableResponse describeGlobalTable(DescribeGlobalTableRequest describeGlobalTableRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeGlobalTable", describeGlobalTableRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeGlobalTableSettingsResponse describeGlobalTableSettings(DescribeGlobalTableSettingsRequest describeGlobalTableSettingsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeGlobalTableSettings", describeGlobalTableSettingsRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }
//
//    @Trace
//    public DescribeKinesisStreamingDestinationResponse describeKinesisStreamingDestination(DescribeKinesisStreamingDestinationRequest describeKinesisStreamingDestinationRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeKinesisStreamingDestination", describeKinesisStreamingDestinationRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public DescribeLimitsResponse describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeLimits", null, endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeTableReplicaAutoScalingResponse describeTableReplicaAutoScaling(DescribeTableReplicaAutoScalingRequest describeTableReplicaAutoScalingRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTableReplicaAutoScaling", describeTableReplicaAutoScalingRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeTimeToLiveResponse describeTimeToLive(DescribeTimeToLiveRequest describeTimeToLiveRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTimeToLive", describeTimeToLiveRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public DisableKinesisStreamingDestinationResponse disableKinesisStreamingDestination(DisableKinesisStreamingDestinationRequest disableKinesisStreamingDestinationRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "disableKinesisStreamingDestination", disableKinesisStreamingDestinationRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public EnableKinesisStreamingDestinationResponse enableKinesisStreamingDestination(EnableKinesisStreamingDestinationRequest enableKinesisStreamingDestinationRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "enableKinesisStreamingDestination", enableKinesisStreamingDestinationRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ExecuteStatementResponse executeStatement(ExecuteStatementRequest executeStatementRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "executeStatement", null, endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ExecuteTransactionResponse executeTransaction(ExecuteTransactionRequest executeTransactionRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "executeTransaction", null, endpoint);
        return Weaver.callOriginal();
    }
//
//    @Trace
//    public ExportTableToPointInTimeResponse exportTableToPointInTime(ExportTableToPointInTimeRequest exportTableToPointInTimeRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "exportTableToPointInTime", exportTableToPointInTimeRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public ListBackupsResponse listBackups(ListBackupsRequest listBackupsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listBackups", listBackupsRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListContributorInsightsResponse listContributorInsights(ListContributorInsightsRequest listContributorInsightsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listContributorInsights", listContributorInsightsRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

//    @Trace
//    public ListExportsResponse listExports(ListExportsRequest listExportsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listExports", listExportsRequest.tablename(), endpoint);
//        return Weaver.callOriginal();
//    }

//    @Trace
//    public ListExportsIterable listExportsPaginator(ListExportsRequest listExportsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listExportsPaginator", listExportsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public ListGlobalTablesResponse listGlobalTables(ListGlobalTablesRequest listGlobalTablesRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listGlobalTables", listGlobalTablesRequest.exclusiveStartGlobalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ListTablesIterable listTablesPaginator(ListTablesRequest listTablesRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTablesPaginator", listTablesRequest.exclusiveStartTableName(), endpoint);
        return Weaver.callOriginal();
    }
//
//    @Trace
//    public ListTagsOfResourceResponse listTagsOfResource(ListTagsOfResourceRequest listTagsOfResourceRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTagsOfResource", listTagsOfResourceRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public QueryResponse query(QueryRequest queryRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "query", queryRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

//    @Trace
//    public QueryIterable queryPaginator(QueryRequest queryRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "queryPaginator", queryRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public RestoreTableFromBackupResponse restoreTableFromBackup(RestoreTableFromBackupRequest restoreTableFromBackupRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "restoreTableFromBackup", restoreTableFromBackupRequest.targetTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public RestoreTableToPointInTimeResponse restoreTableToPointInTime(RestoreTableToPointInTimeRequest restoreTableToPointInTimeRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "restoreTableToPointInTime", restoreTableToPointInTimeRequest.targetTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public ScanIterable scanPaginator(ScanRequest scanRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scanPaginator", scanRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

//    @Trace
//    public TagResourceResponse tagResource(TagResourceRequest tagResourceRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "tagResource", tagResourceRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
//    @Trace
//    public TransactGetItemsResponse transactGetItems(TransactGetItemsRequest transactGetItemsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "transactGetItems", transactGetItemsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
//    @Trace
//    public TransactWriteItemsResponse transactWriteItems(TransactWriteItemsRequest transactWriteItemsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "transactWriteItems", transactWriteItemsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

//    @Trace
//    public UntagResourceResponse untagResource(UntagResourceRequest untagResourceRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "untagResource", untagResourceRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
//    @Trace
//    public UpdateContinuousBackupsResponse updateContinuousBackups(UpdateContinuousBackupsRequest updateContinuousBackupsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateContinuousBackups", updateContinuousBackupsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }
//
//    @Trace
//    public UpdateContributorInsightsResponse updateContributorInsights(UpdateContributorInsightsRequest updateContributorInsightsRequest) {
//        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
//        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateContributorInsights", updateContributorInsightsRequest.tableName(), endpoint);
//        return Weaver.callOriginal();
//    }

    @Trace
    public UpdateGlobalTableResponse updateGlobalTable(UpdateGlobalTableRequest updateGlobalTableRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateGlobalTable", updateGlobalTableRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateGlobalTableSettingsResponse updateGlobalTableSettings(UpdateGlobalTableSettingsRequest updateGlobalTableSettingsRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateGlobalTableSettings", updateGlobalTableSettingsRequest.globalTableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateItemResponse updateItem(UpdateItemRequest updateItemRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateItem", updateItemRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateTableResponse updateTable(UpdateTableRequest updateTableRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTable", updateTableRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateTableReplicaAutoScalingResponse updateTableReplicaAutoScaling(UpdateTableReplicaAutoScalingRequest updateTableReplicaAutoScalingRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTableReplicaAutoScaling", updateTableReplicaAutoScalingRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateTimeToLiveResponse updateTimeToLive(UpdateTimeToLiveRequest updateTimeToLiveRequest) {
        URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTimeToLive", updateTimeToLiveRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }
}