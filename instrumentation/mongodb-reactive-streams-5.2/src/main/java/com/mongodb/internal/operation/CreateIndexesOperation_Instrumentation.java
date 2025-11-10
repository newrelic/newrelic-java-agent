/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.internal.operation;

import com.mongodb.MongoNamespace;
import com.mongodb.WriteConcern;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.binding.AsyncWriteBinding;
import com.mongodb.internal.bulk.IndexRequest;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

import java.util.List;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.internal.operation.CreateIndexesOperation")
public class CreateIndexesOperation_Instrumentation<T> implements AsyncWriteOperation<T> {

    @NewField
    protected String collectionName;

    @NewField
    protected String databaseName;

    @NewField
    protected String operationName;

    public CreateIndexesOperation_Instrumentation(final MongoNamespace namespace, final List<IndexRequest> requests,
            final WriteConcern writeConcern) {
        this.collectionName = namespace.getCollectionName();
        this.databaseName = namespace.getDatabaseName();
        this.operationName = MongoUtil.OP_CREATE_INDEXES;
    }

        @Override
    public void executeAsync(AsyncWriteBinding binding, SingleResultCallback<T> callback) {
        callback = MongoUtil.instrumentSingleResultCallback(callback, collectionName, operationName, databaseName,
                MongoUtil.getHostBasedOnDatabaseName(databaseName));
        Weaver.callOriginal();
    }
}
