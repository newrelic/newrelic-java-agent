/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package software.amazon.dax;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.dynamodb_v2.DAXClientConfigCache;
import com.nr.instrumentation.dynamodb_v2.DAXUtil;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Weave(originalName = "software.amazon.dax.MetricAsyncClient", type = MatchType.ExactClass)
abstract class MetricAsyncClient_Instrumentation {

    MetricAsyncClient_Instrumentation(Configuration configuration) {
        NewRelic.getAgent().getLogger().log(Level.FINEST, "AWSDAX: Construction of new MetricAsyncClient");
        DAXClientConfigCache.storeConfiguration(this, configuration);
    }

    @Trace
    public CompletableFuture<GetItemResponse> getItem(GetItemRequest getItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "getItem", getItemRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<PutItemResponse> putItem(PutItemRequest putItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "putItem", putItemRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DeleteItemResponse> deleteItem(DeleteItemRequest deleteItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "deleteItem", deleteItemRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<UpdateItemResponse> updateItem(UpdateItemRequest updateItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "updateItem", updateItemRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<BatchGetItemResponse> batchGetItem(BatchGetItemRequest batchGetItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "batchGetItem", "batch", this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<BatchWriteItemResponse> batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "batchWriteItem", "batch", this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<QueryResponse> query(QueryRequest queryRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "query", queryRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<ScanResponse> scan(ScanRequest scanRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "scan", scanRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<TransactGetItemsResponse> transactGetItems(TransactGetItemsRequest transactGetItemsRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "transactGetItems", "transaction", this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<TransactWriteItemsResponse> transactWriteItems(TransactWriteItemsRequest transactWriteItemsRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(this);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "transactWriteItems", "transaction", this, config);
        return Weaver.callOriginal();
    }
}
