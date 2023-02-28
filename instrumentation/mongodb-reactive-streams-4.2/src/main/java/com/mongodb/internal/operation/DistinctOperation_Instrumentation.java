/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.internal.operation;

import com.mongodb.MongoNamespace;
import com.mongodb.internal.async.AsyncBatchCursor;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.binding.AsyncReadBinding;
import com.mongodb.internal.binding.ReadBinding;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;
import org.bson.codecs.Decoder;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.internal.operation.DistinctOperation")
public class DistinctOperation_Instrumentation<T> implements AsyncReadOperation<AsyncBatchCursor<T>>, ReadOperation<BatchCursor<T>> {

    @NewField
    protected String collectionName;

    @NewField
    protected String databaseName;

    @NewField
    protected String operationName;

    public DistinctOperation_Instrumentation(final MongoNamespace namespace, final String fieldName, final Decoder<T> decoder) {
        this.collectionName = namespace.getCollectionName();
        this.databaseName = namespace.getDatabaseName();
        this.operationName = MongoUtil.OP_DISTINCT;
    }

    @Override
    public void executeAsync(AsyncReadBinding binding, SingleResultCallback<AsyncBatchCursor<T>> callback) {
        callback = MongoUtil.instrumentSingleResultCallback(callback, collectionName, operationName, databaseName,
                MongoUtil.getHostBasedOnDatabaseName(databaseName));
        Weaver.callOriginal();
    }

    @Override
    public BatchCursor<T> execute(ReadBinding binding) {
        return Weaver.callOriginal();
    }
}