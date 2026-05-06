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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Weave(originalName = "software.amazon.dax.DelegateSyncClient", type = MatchType.ExactClass)
public class DelegateSyncClient_Instrumentation {

    // The underlying async client that the sync client wraps. This is used
    // to lookup config information
    private final DynamoDbAsyncClient client = Weaver.callOriginal();

    @Trace
    public GetItemResponse getItem(GetItemRequest getItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "getItem", getItemRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public PutItemResponse putItem(PutItemRequest putItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "putItem", putItemRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteItemResponse deleteItem(DeleteItemRequest deleteItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "deleteItem", deleteItemRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateItemResponse updateItem(UpdateItemRequest updateItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "updateItem", updateItemRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public BatchGetItemResponse batchGetItem(BatchGetItemRequest batchGetItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "batchGetItem", "batch", this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public BatchWriteItemResponse batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "batchWriteItem", "batch", this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse query(QueryRequest queryRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "query", queryRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public ScanResponse scan(ScanRequest scanRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "scan", scanRequest.tableName(), this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public TransactGetItemsResponse transactGetItems(TransactGetItemsRequest transactGetItemsRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "transactGetItems", "transaction", this, config);
        return Weaver.callOriginal();
    }

    @Trace
    public TransactWriteItemsResponse transactWriteItems(TransactWriteItemsRequest transactWriteItemsRequest) {
        Configuration config = DAXClientConfigCache.getConfiguration(client);
        DAXUtil.recordExternal(NewRelic.getAgent().getTracedMethod(), "transactWriteItems", "transaction", this, config);
        return Weaver.callOriginal();
    }
}
