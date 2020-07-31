/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.client;

import java.util.List;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

@Weave(type = MatchType.Interface, originalName = "com.mongodb.client.MongoCollection")
public abstract class MongoCollection_Instrumentation<TDocument> {

    @NewField
    public ServerAddress address = new ServerAddress("unknown");

    public abstract MongoNamespace getNamespace();

    @Trace(leaf = true)
    public long count(Bson filter, CountOptions options) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_COUNT);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <TResult> DistinctIterable<TResult> distinct(String fieldName, Class<TResult> resultClass) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_DISTINCT);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <TResult> FindIterable<TResult> find(Bson filter, Class<TResult> resultClass) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_FIND);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> pipeline, Class<TResult> resultClass) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_AGGREGATE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <TResult> MapReduceIterable<TResult> mapReduce(String mapFunction, String reduceFunction,
            Class<TResult> resultClass) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_MAP_REDUCE);
        return Weaver.callOriginal();
    }

    /*
     * This eventually calls MixedBulkWriteOperation, which is where we record metrics for each individual operation.
     * Hence no leaf = true here.
     */
    @Trace
    public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_BULK_WRITE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void insertOne(TDocument document) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_INSERT);
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void insertMany(List<? extends TDocument> documents, InsertManyOptions options) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_INSERT_MANY);
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public DeleteResult deleteOne(Bson filter) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_DELETE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public DeleteResult deleteMany(Bson filter) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_DELETE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public UpdateResult replaceOne(final Bson filter, final TDocument replacement, final UpdateOptions updateOptions) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_REPLACE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public UpdateResult updateOne(Bson filter, Bson update, UpdateOptions updateOptions) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_UPDATE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public UpdateResult updateMany(Bson filter, Bson update, UpdateOptions updateOptions) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_UPDATE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public TDocument findOneAndDelete(Bson filter, FindOneAndDeleteOptions options) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_FIND_AND_DELETE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public TDocument findOneAndReplace(final Bson filter, final TDocument replacement,
            final FindOneAndReplaceOptions options) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_FIND_AND_REPLACE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public TDocument findOneAndUpdate(final Bson filter, final Bson update, final FindOneAndUpdateOptions options) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_FIND_AND_UPDATE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void drop() {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_DROP_COLLECTION);
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public List<String> createIndexes(List<IndexModel> indexes) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_REPLACE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <TResult> ListIndexesIterable<TResult> listIndexes(Class<TResult> resultClass) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_LIST_INDEX);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void dropIndex(String indexName) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_DROP_INDEX);
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void dropIndex(Bson keys) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_DROP_INDEX);
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void renameCollection(MongoNamespace newCollectionNamespace, RenameCollectionOptions renameCollectionOptions) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_RENAME_COLLECTION);
        Weaver.callOriginal();
    }

    private void instrument(TracedMethod method, String operationName) {
        DatastoreParameters params = DatastoreParameters
                .product(DatastoreVendor.MongoDB.name())
                .collection(getNamespace().getCollectionName())
                .operation(operationName)
                .instance(address.getHost(), address.getPort())
                .databaseName(getNamespace().getDatabaseName())
                .build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
    }

    public MongoCollection_Instrumentation<TDocument> withCodecRegistry(final CodecRegistry codecRegistry) {
        MongoCollection_Instrumentation<TDocument> collection = Weaver.callOriginal();
        collection.address = address;
        return collection;
    }

    public <NewTDocument> MongoCollection_Instrumentation<NewTDocument> withDocumentClass(final Class<NewTDocument> clazz) {
        MongoCollection_Instrumentation<NewTDocument> collection = Weaver.callOriginal();
        collection.address = address;
        return collection;
    }

    public MongoCollection_Instrumentation<TDocument> withReadPreference(final ReadPreference readPreference) {
        MongoCollection_Instrumentation<TDocument> collection = Weaver.callOriginal();
        collection.address = address;
        return collection;
    }

    public MongoCollection_Instrumentation<TDocument> withWriteConcern(final WriteConcern writeConcern) {
        MongoCollection_Instrumentation<TDocument> collection = Weaver.callOriginal();
        collection.address = address;
        return collection;
    }

    // these methods are declared for test compatibility
    public abstract void insertMany(List<? extends TDocument> documents);

    public abstract FindIterable<TDocument> find();

    public abstract FindIterable<TDocument> find(Bson filter);

    public abstract UpdateResult updateOne(Bson filter, Bson update);

    public abstract UpdateResult updateMany(Bson filter, Bson update);

    public abstract BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests);

    public abstract long count();

}
