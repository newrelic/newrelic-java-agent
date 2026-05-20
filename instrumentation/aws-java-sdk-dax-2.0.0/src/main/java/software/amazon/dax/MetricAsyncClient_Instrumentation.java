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
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.dynamodb_dax.DAXUtil;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Weave(originalName = "software.amazon.dax.MetricAsyncClient", type = MatchType.ExactClass)
abstract class MetricAsyncClient_Instrumentation {

    @NewField
    Configuration configuration;

    MetricAsyncClient_Instrumentation(Configuration configuration) {
        NewRelic.getAgent().getLogger().log(Level.FINEST, "AWSDAX: Construction of new MetricAsyncClient");
        this.configuration = configuration;
    }

    @Trace
    public CompletableFuture<GetItemResponse> getItem(GetItemRequest getItemRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "getItem", getItemRequest.tableName(), this, configuration);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<PutItemResponse> putItem(PutItemRequest putItemRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "putItem", putItemRequest.tableName(), this, configuration);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DeleteItemResponse> deleteItem(DeleteItemRequest deleteItemRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "deleteItem", deleteItemRequest.tableName(), this, configuration);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<UpdateItemResponse> updateItem(UpdateItemRequest updateItemRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "updateItem", updateItemRequest.tableName(), this, configuration);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<BatchGetItemResponse> batchGetItem(BatchGetItemRequest batchGetItemRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "batchGetItem", "batch", this, configuration);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<BatchWriteItemResponse> batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "batchWriteItem", "batch", this, configuration);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<QueryResponse> query(QueryRequest queryRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "query", queryRequest.tableName(), this, configuration);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<ScanResponse> scan(ScanRequest scanRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "scan", scanRequest.tableName(), this, configuration);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<TransactGetItemsResponse> transactGetItems(TransactGetItemsRequest transactGetItemsRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "transactGetItems", "transaction", this, configuration);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<TransactWriteItemsResponse> transactWriteItems(TransactWriteItemsRequest transactWriteItemsRequest) {
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "transactWriteItems", "transaction", this, configuration);
        return Weaver.callOriginal();
    }
}
