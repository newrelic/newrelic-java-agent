/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.internal.async.client;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.internal.client.model.AggregationLevel;
import com.mongodb.lang.Nullable;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.mongo.MongoUtil;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.util.List;

@Weave(type = MatchType.ExactClass, originalName = "com/mongodb/internal/async/client/AsyncAggregateIterableImpl")
abstract class AsyncAggregateIterableImpl_Instrumentation<TDocument, TResult> extends AsyncMongoIterableImpl_Instrumentation<TResult> {

    AsyncAggregateIterableImpl_Instrumentation(@Nullable final AsyncClientSession clientSession, final String databaseName,
            final Class<TDocument> documentClass, final Class<TResult> resultClass, final CodecRegistry codecRegistry,
            final ReadPreference readPreference, final ReadConcern readConcern, final WriteConcern writeConcern,
            final OperationExecutor executor, final List<? extends Bson> pipeline,
            final AggregationLevel aggregationLevel, final boolean retryReads) {
        super(clientSession, executor, readConcern, readPreference, retryReads);
        super.collectionName = "";
        super.databaseName = databaseName;
        super.operationName = MongoUtil.OP_AGGREGATE;
    }
}
