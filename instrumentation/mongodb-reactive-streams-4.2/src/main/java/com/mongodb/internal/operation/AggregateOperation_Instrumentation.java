/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.internal.operation;

import com.mongodb.MongoNamespace;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.binding.AsyncReadBinding;
import com.mongodb.internal.client.model.AggregationLevel;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;
import org.bson.BsonDocument;
import org.bson.codecs.Decoder;

import java.util.List;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.internal.operation.AggregateOperation")
public class AggregateOperation_Instrumentation<T> implements AsyncReadOperation<T> {

    @NewField
    protected String collectionName;

    @NewField
    protected String databaseName;

    @NewField
    protected String operationName;

    public AggregateOperation_Instrumentation(final MongoNamespace namespace, final List<BsonDocument> pipeline, final Decoder<T> decoder) {
        assignMetadataFields(namespace);
    }

    public AggregateOperation_Instrumentation(final MongoNamespace namespace, final List<BsonDocument> pipeline, final Decoder<T> decoder,
            final AggregationLevel aggregationLevel) {
        assignMetadataFields(namespace);
    }

    @Override
    public void executeAsync(AsyncReadBinding binding, SingleResultCallback<T> callback) {
        callback = MongoUtil.instrumentSingleResultCallback(callback, collectionName, operationName, databaseName,
                MongoUtil.getHostBasedOnDatabaseName(databaseName));
        Weaver.callOriginal();
    }

    private void assignMetadataFields(MongoNamespace namespace) {
        this.collectionName = namespace.getCollectionName();
        this.databaseName = namespace.getDatabaseName();
        this.operationName = MongoUtil.OP_AGGREGATE;
    }
}
