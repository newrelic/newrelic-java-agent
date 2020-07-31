/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.dynamodbv2;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.handlers.AsyncHandler_Instrumentation;
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
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.Future;

@Weave(originalName = "com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient")
public abstract class AmazonDynamoDBAsyncClient_Instrumentation extends AmazonDynamoDBClient {

    @Trace
    public Future<BatchGetItemResult> batchGetItemAsync(final BatchGetItemRequest batchGetItemRequest,
            final AsyncHandler_Instrumentation<BatchGetItemRequest, BatchGetItemResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(batchGetItemRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<BatchWriteItemResult> batchWriteItemAsync(final BatchWriteItemRequest batchWriteItemRequest,
            final AsyncHandler_Instrumentation<BatchWriteItemRequest, BatchWriteItemResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(batchWriteItemRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<CreateTableResult> createTableAsync(final CreateTableRequest createTableRequest,
            final AsyncHandler_Instrumentation<CreateTableRequest, CreateTableResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(createTableRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DeleteItemResult> deleteItemAsync(final DeleteItemRequest deleteItemRequest,
            final AsyncHandler_Instrumentation<DeleteItemRequest, DeleteItemResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(deleteItemRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DeleteTableResult> deleteTableAsync(final DeleteTableRequest deleteTableRequest,
            final AsyncHandler_Instrumentation<DeleteTableRequest, DeleteTableResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(deleteTableRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DescribeTableResult> describeTableAsync(final DescribeTableRequest describeTableRequest,
            final AsyncHandler_Instrumentation<DescribeTableRequest, DescribeTableResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(describeTableRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<GetItemResult> getItemAsync(final GetItemRequest getItemRequest,
            final AsyncHandler_Instrumentation<GetItemRequest, GetItemResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(getItemRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<ListTablesResult> listTablesAsync(final ListTablesRequest listTablesRequest,
            final AsyncHandler_Instrumentation<ListTablesRequest, ListTablesResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(listTablesRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<PutItemResult> putItemAsync(final PutItemRequest putItemRequest,
            final AsyncHandler_Instrumentation<PutItemRequest, PutItemResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(putItemRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<QueryResult> queryAsync(final QueryRequest queryRequest,
            final AsyncHandler_Instrumentation<QueryRequest, QueryResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(queryRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<ScanResult> scanAsync(final ScanRequest scanRequest,
            final AsyncHandler_Instrumentation<ScanRequest, ScanResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(scanRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<UpdateItemResult> updateItemAsync(final UpdateItemRequest updateItemRequest,
            final AsyncHandler_Instrumentation<UpdateItemRequest, UpdateItemResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(updateItemRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<UpdateTableResult> updateTableAsync(final UpdateTableRequest updateTableRequest,
            final AsyncHandler_Instrumentation<UpdateTableRequest, UpdateTableResult> asyncHandler) {
        storeToken(asyncHandler);
        storeToken(updateTableRequest);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DescribeLimitsResult> describeLimitsAsync(final DescribeLimitsRequest request,
            final AsyncHandler_Instrumentation<DescribeLimitsRequest, DescribeLimitsResult> asyncHandler) {
        storeToken(request);
        storeToken(asyncHandler);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DescribeTimeToLiveResult> describeTimeToLiveAsync(final DescribeTimeToLiveRequest request,
            final AsyncHandler_Instrumentation<DescribeTimeToLiveRequest, DescribeTimeToLiveResult> asyncHandler) {
        storeToken(request);
        storeToken(asyncHandler);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<ListTagsOfResourceResult> listTagsOfResourceAsync(final ListTagsOfResourceRequest request,
            final AsyncHandler_Instrumentation<ListTagsOfResourceRequest, ListTagsOfResourceResult> asyncHandler) {
        storeToken(request);
        storeToken(asyncHandler);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<TagResourceResult> tagResourceAsync(final TagResourceRequest request,
            final AsyncHandler_Instrumentation<TagResourceRequest, TagResourceResult> asyncHandler) {
        storeToken(request);
        storeToken(asyncHandler);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<UntagResourceResult> untagResourceAsync(final UntagResourceRequest request,
            final AsyncHandler_Instrumentation<UntagResourceRequest, UntagResourceResult> asyncHandler) {
        storeToken(request);
        storeToken(asyncHandler);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<UpdateTimeToLiveResult> updateTimeToLiveAsync(final UpdateTimeToLiveRequest request,
            final AsyncHandler_Instrumentation<UpdateTimeToLiveRequest, UpdateTimeToLiveResult> asyncHandler) {
        storeToken(request);
        storeToken(asyncHandler);
        return Weaver.callOriginal();
    }

    private void storeToken(AmazonWebServiceRequest request) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            request.token = NewRelic.getAgent().getTransaction().getToken();
        }
    }

    private void storeToken(AsyncHandler_Instrumentation asyncHandler) {
        if (asyncHandler != null && AgentBridge.getAgent().getTransaction(false) != null) {
            asyncHandler.token = NewRelic.getAgent().getTransaction().getToken();
        }
    }
}
