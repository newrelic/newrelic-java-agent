/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.reactivestreams.client;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CreateIndexOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.internal.MongoOperationPublisher;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;

import java.util.List;

import static com.nr.agent.mongo.MongoUtil.CUSTOM;
import static com.nr.agent.mongo.MongoUtil.OP_AGGREGATE;
import static com.nr.agent.mongo.MongoUtil.OP_BULK_WRITE;
import static com.nr.agent.mongo.MongoUtil.OP_CREATE_INDEXES;
import static com.nr.agent.mongo.MongoUtil.OP_DELETE_MANY;
import static com.nr.agent.mongo.MongoUtil.OP_DELETE_ONE;
import static com.nr.agent.mongo.MongoUtil.OP_DROP_COLLECTION;
import static com.nr.agent.mongo.MongoUtil.OP_FIND;
import static com.nr.agent.mongo.MongoUtil.OP_FIND_ONE_AND_DELETE;
import static com.nr.agent.mongo.MongoUtil.OP_FIND_ONE_AND_REPLACE;
import static com.nr.agent.mongo.MongoUtil.OP_FIND_ONE_AND_UPDATE;
import static com.nr.agent.mongo.MongoUtil.OP_INSERT_MANY;
import static com.nr.agent.mongo.MongoUtil.OP_INSERT_ONE;
import static com.nr.agent.mongo.MongoUtil.OP_MAP_REDUCE;
import static com.nr.agent.mongo.MongoUtil.OP_REPLACE_ONE;
import static com.nr.agent.mongo.MongoUtil.OP_UPDATE_MANY;
import static com.nr.agent.mongo.MongoUtil.OP_UPDATE_ONE;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.reactivestreams.client.internal.MongoCollectionImpl")
class MongoCollectionImpl_Instrumentation<T> {

    /**
     * To prevent auto-generation of zero arg constructor which causes a weave violation
     */
    MongoCollectionImpl_Instrumentation(MongoOperationPublisher<T> mongoOperationPublisher) {
        //no-op
    }

    @Trace(leaf = true)
    public <T> FindPublisher<T> find(final Bson filter, final Class<T> clazz) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <T> FindPublisher<T> find(final ClientSession clientSession, final Bson filter,
            final Class<T> clazz) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <T> AggregatePublisher<T> aggregate(final List<? extends Bson> pipeline, final Class<T> clazz) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_AGGREGATE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <T> AggregatePublisher<T> aggregate(final ClientSession clientSession, final List<? extends Bson> pipeline,
            final Class<T> clazz) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_AGGREGATE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <T> MapReducePublisher<T> mapReduce(final String mapFunction, final String reduceFunction,
            final Class<T> clazz) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_MAP_REDUCE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <T> MapReducePublisher<T> mapReduce(final ClientSession clientSession, final String mapFunction,
            final String reduceFunction, final Class<T> clazz) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_MAP_REDUCE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<BulkWriteResult> bulkWrite(final List<? extends WriteModel<? extends T>> requests,
            final BulkWriteOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_BULK_WRITE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<BulkWriteResult> bulkWrite(final ClientSession clientSession, final List<? extends WriteModel<? extends T>> requests,
            final BulkWriteOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_BULK_WRITE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<InsertOneResult> insertOne(final T document, final InsertOneOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_INSERT_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<InsertOneResult> insertOne(final ClientSession clientSession, final T document, final InsertOneOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_INSERT_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<InsertManyResult> insertMany(final List<? extends T> documents, final InsertManyOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_INSERT_MANY);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<InsertManyResult> insertMany(final ClientSession clientSession, final List<? extends T> documents,
            final InsertManyOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_INSERT_MANY);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteOne(final Bson filter, final DeleteOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_DELETE_ONE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteOne(final ClientSession clientSession, final Bson filter, final DeleteOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_DELETE_ONE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteMany(final Bson filter, final DeleteOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_DELETE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteMany(final ClientSession clientSession, final Bson filter, final DeleteOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_DELETE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> replaceOne(final Bson filter, final T replacement, final ReplaceOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_REPLACE_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> replaceOne(final ClientSession clientSession, final Bson filter, final T replacement,
            final ReplaceOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_REPLACE_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final Bson filter, final Bson update, final UpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_UPDATE_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final ClientSession clientSession, final Bson filter, final Bson update,
            final UpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_UPDATE_ONE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final Bson filter, final List<? extends Bson> update, final UpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_UPDATE_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final ClientSession clientSession, final Bson filter, final List<? extends Bson> update,
            final UpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_UPDATE_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final Bson filter, final Bson update, final UpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_UPDATE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final ClientSession clientSession, final Bson filter, final Bson update,
            final UpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_UPDATE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final Bson filter, final List<? extends Bson> update, final UpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_UPDATE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final ClientSession clientSession, final Bson filter, final List<? extends Bson> update,
            final UpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_UPDATE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<T> findOneAndDelete(final Bson filter, final FindOneAndDeleteOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND_ONE_AND_DELETE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<T> findOneAndDelete(final ClientSession clientSession, final Bson filter, final FindOneAndDeleteOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND_ONE_AND_DELETE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<T> findOneAndReplace(final Bson filter, final T replacement, final FindOneAndReplaceOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND_ONE_AND_REPLACE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<T> findOneAndReplace(final ClientSession clientSession, final Bson filter, final T replacement,
            final FindOneAndReplaceOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND_ONE_AND_REPLACE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<T> findOneAndUpdate(final Bson filter, final Bson update, final FindOneAndUpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND_ONE_AND_UPDATE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<T> findOneAndUpdate(final ClientSession clientSession, final Bson filter, final Bson update,
            final FindOneAndUpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND_ONE_AND_UPDATE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<T> findOneAndUpdate(final Bson filter, final List<? extends Bson> update,
            final FindOneAndUpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND_ONE_AND_UPDATE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<T> findOneAndUpdate(final ClientSession clientSession, final Bson filter,
            final List<? extends Bson> update,
            final FindOneAndUpdateOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_FIND_ONE_AND_UPDATE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<Void> drop() {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_DROP_COLLECTION);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<Void> drop(final ClientSession clientSession) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_DROP_COLLECTION);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<String> createIndex(final Bson key, final IndexOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_CREATE_INDEXES);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<String> createIndex(final ClientSession clientSession, final Bson key, final IndexOptions options) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_CREATE_INDEXES);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<String> createIndexes(final List<IndexModel> indexes, final CreateIndexOptions createIndexOptions) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_CREATE_INDEXES);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<String> createIndexes(final ClientSession clientSession, final List<IndexModel> indexes,
            final CreateIndexOptions createIndexOptions) {
        NewRelic.getAgent().getTracedMethod().setMetricName(CUSTOM, "ReactiveMongoCollection", OP_CREATE_INDEXES);
        return Weaver.callOriginal();
    }

}