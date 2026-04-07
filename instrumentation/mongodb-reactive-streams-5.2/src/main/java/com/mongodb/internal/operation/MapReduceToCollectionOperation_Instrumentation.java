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
import org.bson.BsonJavaScript;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.internal.operation.MapReduceToCollectionOperation")
public class MapReduceToCollectionOperation_Instrumentation implements AsyncWriteOperation<MapReduceStatistics> {
    @NewField
    protected String operationName;

    public MapReduceToCollectionOperation_Instrumentation(final MongoNamespace namespace, final BsonJavaScript mapFunction,
            final BsonJavaScript reduceFunction, final String collectionName,
            final WriteConcern writeConcern) {
        this.operationName = MongoUtil.OP_MAP_REDUCE;
    }

    public String getDatabaseName() {
        return Weaver.callOriginal();
    }

    public String getCollectionName() {
        return Weaver.callOriginal();
    }

    @Override
    public void executeAsync(AsyncWriteBinding binding, SingleResultCallback callback) {
        callback = MongoUtil.instrumentSingleResultCallback(callback, getCollectionName(), operationName, getDatabaseName(),
                MongoUtil.getHostBasedOnDatabaseName(getDatabaseName()));
        Weaver.callOriginal();
    }
}
