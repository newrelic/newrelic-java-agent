/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.internal.operation;

import com.mongodb.MongoNamespace;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.binding.AsyncWriteBinding;
import com.mongodb.internal.bulk.WriteRequest;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

import java.util.List;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.internal.operation.MixedBulkWriteOperation")
public class MixedBulkWriteOperation_Instrumentation implements AsyncWriteOperation<BulkWriteResult> {

    @NewField
    protected String collectionName;

    @NewField
    protected String databaseName;

    @NewField
    protected String operationName;

    public MixedBulkWriteOperation_Instrumentation(final MongoNamespace namespace, final List<? extends WriteRequest> writeRequests,
            final boolean ordered, final WriteConcern writeConcern, final boolean retryWrites) {
        this.collectionName = namespace.getCollectionName();
        this.databaseName = namespace.getDatabaseName();
    }

    @Override
    public void executeAsync(AsyncWriteBinding binding, SingleResultCallback<BulkWriteResult> callback) {
        callback = MongoUtil.instrumentSingleResultCallback(callback, collectionName, operationName, databaseName,
                MongoUtil.getHostBasedOnDatabaseName(databaseName));
        Weaver.callOriginal();
    }
}
