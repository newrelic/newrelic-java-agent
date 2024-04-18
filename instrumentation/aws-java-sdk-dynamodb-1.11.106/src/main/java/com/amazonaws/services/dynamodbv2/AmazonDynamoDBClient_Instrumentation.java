/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.dynamodbv2;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeLimitsRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeLimitsResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ListTagsOfResourceRequest;
import com.amazonaws.services.dynamodbv2.model.ListTagsOfResourceResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TagResourceRequest;
import com.amazonaws.services.dynamodbv2.model.TagResourceResult;
import com.amazonaws.services.dynamodbv2.model.UntagResourceRequest;
import com.amazonaws.services.dynamodbv2.model.UntagResourceResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTableResult;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveResult;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.dynamodb_1_11_106.DynamoDBMetricUtil;

/**
 * This provides external instrumentation for Amazon's DynamoDB Java API 1.9.0+. Metrics are all generated in
 * {@link DynamoDBMetricUtil} - all that's different from one method to another is the method name.
 */
@Weave(originalName = "com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient")
public abstract class AmazonDynamoDBClient_Instrumentation extends AmazonWebServiceClient {

    public AmazonDynamoDBClient_Instrumentation(ClientConfiguration clientConfiguration) {
        super(clientConfiguration);
    }
    private final AWSCredentialsProvider awsCredentialsProvider = Weaver.callOriginal();

    @Trace(async = true, leaf = true)
    final CreateTableResult executeCreateTable(CreateTableRequest createTableRequest) {
        linkAndExpire(createTableRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "createTable",
                createTableRequest.getTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final BatchGetItemResult executeBatchGetItem(BatchGetItemRequest batchGetItemRequest) {
        linkAndExpire(batchGetItemRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchGetItem", "batch", endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final BatchWriteItemResult executeBatchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
        linkAndExpire(batchWriteItemRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "batchWriteItem", "batch", endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DeleteItemResult executeDeleteItem(DeleteItemRequest deleteItemRequest) {
        linkAndExpire(deleteItemRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteItem",
                deleteItemRequest.getTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DeleteTableResult executeDeleteTable(DeleteTableRequest deleteTableRequest) {
        linkAndExpire(deleteTableRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteTable",
                deleteTableRequest.getTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DescribeLimitsResult executeDescribeLimits(DescribeLimitsRequest describeLimitsRequest) {
        linkAndExpire(describeLimitsRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeLimits", null, endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DescribeTableResult executeDescribeTable(DescribeTableRequest describeTableRequest) {
        linkAndExpire(describeTableRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTable",
                describeTableRequest.getTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DescribeTimeToLiveResult executeDescribeTimeToLive(DescribeTimeToLiveRequest describeTimeToLiveRequest) {
        linkAndExpire(describeTimeToLiveRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "describeTimeToLive",
                describeTimeToLiveRequest.getTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final GetItemResult executeGetItem(GetItemRequest getItemRequest) {
        linkAndExpire(getItemRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "getItem", getItemRequest.getTableName(),
                endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final ListTablesResult executeListTables(ListTablesRequest listTablesRequest) {
        linkAndExpire(listTablesRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTables",
                listTablesRequest.getExclusiveStartTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final ListTagsOfResourceResult executeListTagsOfResource(ListTagsOfResourceRequest listTagsOfResourceRequest) {
        linkAndExpire(listTagsOfResourceRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "listTagsOfResource", null, endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final PutItemResult executePutItem(PutItemRequest putItemRequest) {
        linkAndExpire(putItemRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "putItem", putItemRequest.getTableName(),
                endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final QueryResult executeQuery(QueryRequest queryRequest) {
        linkAndExpire(queryRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "query", queryRequest.getTableName(),
                endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();

    }

    @Trace(async = true, leaf = true)
    final ScanResult executeScan(ScanRequest scanRequest) {
        linkAndExpire(scanRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scan", scanRequest.getTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final TagResourceResult executeTagResource(TagResourceRequest tagResourceRequest) {
        linkAndExpire(tagResourceRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "tagResource", null, endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final UntagResourceResult executeUntagResource(UntagResourceRequest untagResourceRequest) {
        linkAndExpire(untagResourceRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "untagResource", null, endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final UpdateItemResult executeUpdateItem(UpdateItemRequest updateItemRequest) {
        linkAndExpire(updateItemRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateItem",
                updateItemRequest.getTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final UpdateTableResult executeUpdateTable(UpdateTableRequest updateTableRequest) {
        linkAndExpire(updateTableRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTable",
                updateTableRequest.getTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final UpdateTimeToLiveResult executeUpdateTimeToLive(UpdateTimeToLiveRequest updateTimeToLiveRequest) {
       linkAndExpire(updateTimeToLiveRequest);
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "updateTimeToLive",
                updateTimeToLiveRequest.getTableName(), endpoint, awsCredentialsProvider);
        return Weaver.callOriginal();
    }

    private void linkAndExpire(AmazonWebServiceRequest request) {
        if (request.token != null) {
            request.token.linkAndExpire();
            request.token = null;
        };
    }
}
