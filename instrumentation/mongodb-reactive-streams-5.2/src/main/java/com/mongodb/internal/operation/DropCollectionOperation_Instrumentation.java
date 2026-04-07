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
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.internal.operation.DropCollectionOperation")
public class DropCollectionOperation_Instrumentation<T> implements AsyncWriteOperation<T> {

    @NewField
    protected String collectionName;

    @NewField
    protected String databaseName;

    @NewField
    protected String operationName;

    public DropCollectionOperation_Instrumentation(final MongoNamespace namespace, final WriteConcern writeConcern) {
        assignMetadataFields(namespace);
    }

    @Override
    public void executeAsync(AsyncWriteBinding binding, SingleResultCallback<T> callback) {
        callback = MongoUtil.instrumentSingleResultCallback(callback, collectionName, operationName, databaseName,
                MongoUtil.getHostBasedOnDatabaseName(databaseName));
        Weaver.callOriginal();
    }

    private void assignMetadataFields(MongoNamespace namespace) {
        this.collectionName = namespace.getCollectionName();
        this.databaseName = namespace.getDatabaseName();
        this.operationName = MongoUtil.OP_DROP_COLLECTION;
    }
}
