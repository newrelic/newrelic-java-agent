/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.internal.operation;

import com.mongodb.MongoNamespace;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateIndexOptions;
import com.mongodb.client.model.DropIndexOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.internal.client.model.AggregationLevel;
import com.mongodb.internal.client.model.CountStrategy;
import com.mongodb.internal.client.model.FindOptions;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;
import org.bson.conversions.Bson;

import java.util.List;

import static com.nr.agent.mongo.MongoUtil.CUSTOM;
import static com.nr.agent.mongo.MongoUtil.OP_AGGREGATE;
import static com.nr.agent.mongo.MongoUtil.OP_BULK_WRITE;
import static com.nr.agent.mongo.MongoUtil.OP_COUNT;
import static com.nr.agent.mongo.MongoUtil.OP_CREATE_INDEXES;
import static com.nr.agent.mongo.MongoUtil.OP_DISTINCT;
import static com.nr.agent.mongo.MongoUtil.OP_DROP_COLLECTION;
import static com.nr.agent.mongo.MongoUtil.OP_DROP_INDEX;
import static com.nr.agent.mongo.MongoUtil.OP_FIND;
import static com.nr.agent.mongo.MongoUtil.OP_INSERT_MANY;
import static com.nr.agent.mongo.MongoUtil.OP_LIST_COLLECTIONS;
import static com.nr.agent.mongo.MongoUtil.OP_LIST_DATABASES;
import static com.nr.agent.mongo.MongoUtil.OP_LIST_INDEXES;
import static com.nr.agent.mongo.MongoUtil.OP_RENAME_COLLECTION;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.internal.operation.Operations")
public class Operations_Instrumentation<TDocument> {

    /**
     * This covers all variations of the "find" methods in the original class
     */
    @Trace
    private <TResult> FindOperation<TResult> createFindOperation(final MongoNamespace findNamespace, final Bson filter,
            final Class<TResult> resultClass, final FindOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_FIND);
        return Weaver.callOriginal();
    }

    @Trace
    public <TResult> DistinctOperation<TResult> distinct(final String fieldName, final Bson filter,
            final Class<TResult> resultClass, final long maxTimeMS,
            final Collation collation) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_DISTINCT);
        return Weaver.callOriginal();
    }

    @Trace
    public <TResult> AggregateOperation<TResult> aggregate(final List<? extends Bson> pipeline, final Class<TResult> resultClass,
            final long maxTimeMS, final long maxAwaitTimeMS, final Integer batchSize,
            final Collation collation, final Bson hint, final String comment,
            final Boolean allowDiskUse, final AggregationLevel aggregationLevel) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_AGGREGATE);
        return Weaver.callOriginal();
    }

    @Trace
    public MixedBulkWriteOperation_Instrumentation insertMany(final List<? extends TDocument> documents, final InsertManyOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_INSERT_MANY);
        MixedBulkWriteOperation_Instrumentation op = Weaver.callOriginal();
        op.operationName = OP_INSERT_MANY;
        return op;
    }

    @Trace
    public CountOperation count(final Bson filter, final CountOptions options, final CountStrategy countStrategy) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_COUNT);
        return Weaver.callOriginal();
    }

    @Trace
    public DropCollectionOperation dropCollection() {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_DROP_COLLECTION);
        return Weaver.callOriginal();
    }

    @Trace
    public RenameCollectionOperation renameCollection(final MongoNamespace newCollectionNamespace, final RenameCollectionOptions renameCollectionOptions) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_RENAME_COLLECTION);
        return Weaver.callOriginal();
    }

    @Trace
    public CreateIndexesOperation createIndexes(final List<IndexModel> indexes, final CreateIndexOptions createIndexOptions) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_CREATE_INDEXES);
        return Weaver.callOriginal();
    }

    @Trace
    public DropIndexOperation dropIndex(final String indexName, final DropIndexOptions dropIndexOptions) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_DROP_INDEX);
        return Weaver.callOriginal();
    }

    @Trace
    public DropIndexOperation dropIndex(final Bson keys, final DropIndexOptions dropIndexOptions) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_DROP_INDEX);
        return Weaver.callOriginal();
    }

    @Trace
    public <TResult> ListCollectionsOperation<TResult> listCollections(final String databaseName, final Class<TResult> resultClass,
            final Bson filter, final boolean collectionNamesOnly,
            final Integer batchSize, final long maxTimeMS) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_LIST_COLLECTIONS);
        return Weaver.callOriginal();
    }

    @Trace
    public <TResult> ListDatabasesOperation<TResult> listDatabases(final Class<TResult> resultClass, final Bson filter,
            final Boolean nameOnly, final long maxTimeMS,
            final Boolean authorizedDatabasesOnly) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_LIST_DATABASES);
        return Weaver.callOriginal();
    }

    @Trace
    public <TResult> ListIndexesOperation<TResult> listIndexes(final Class<TResult> resultClass, final Integer batchSize, final long maxTimeMS) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", OP_LIST_INDEXES);
        return Weaver.callOriginal();
    }

    /**
     * Because all the write operations proxy to this method, we can instrument a single method and assign
     * the operation name passed on the length of the <code>requests</code> {@link java.util.List List}.
     * <ul>
     *     <li>if the list has a single entry, interrogate its type and assign the operation name accordingly</li>
     *     <li>if the list has more than one entry, the operation name will be <code>OP_BULK_WRITE</code></li>
     * </ul>
     */
    @Trace
    public MixedBulkWriteOperation_Instrumentation bulkWrite(final List<? extends WriteModel<? extends TDocument>> requests, final BulkWriteOptions options) {
        String operationName  = requests.size() > 1 ? OP_BULK_WRITE : MongoUtil.determineBulkWriteOperation(requests.get(0));
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoOperation", operationName);
        MixedBulkWriteOperation_Instrumentation op = Weaver.callOriginal();
        op.operationName = operationName;
        return op;
    }
}
