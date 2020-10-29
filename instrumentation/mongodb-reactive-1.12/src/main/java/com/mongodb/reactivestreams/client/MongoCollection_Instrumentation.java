package com.mongodb.reactivestreams.client;

import com.mongodb.MongoNamespace;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;

import com.mongodb.client.model.CreateIndexOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.DropIndexOptions;

import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;

import java.util.List;

@Weave(type = MatchType.Interface, originalName = "com.mongodb.reactivestreams.client.MongoCollection")
public abstract class MongoCollection_Instrumentation<TDocument> {

    public MongoNamespace getNamespace() {
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <TResult> FindPublisher<TResult> find(final Bson filter, final Class<TResult> clazz) {
        reportAsExternal("find");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> FindPublisher<TResult> find(final ClientSession clientSession, final Bson filter,
                                                 final Class<TResult> clazz) {
        reportAsExternal("find");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> AggregatePublisher<TResult> aggregate(final List<? extends Bson> pipeline,
                                                           final Class<TResult> clazz) {
        reportAsExternal("aggregate");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> AggregatePublisher<TResult> aggregate(final ClientSession clientSession,
                                                           final List<? extends Bson> pipeline,
                                                           final Class<TResult> clazz) {
        reportAsExternal("aggregate");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> MapReducePublisher<TResult> mapReduce(final String mapFunction, final String reduceFunction,
                                                           final Class<TResult> clazz) {
        reportAsExternal("mapReduce");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> MapReducePublisher<TResult> mapReduce(final ClientSession clientSession, final String mapFunction,
                                                           final String reduceFunction, final Class<TResult> clazz) {

        reportAsExternal("mapReduce");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<BulkWriteResult> bulkWrite(final List<? extends WriteModel<? extends TDocument>> requests,
                                                final BulkWriteOptions options) {

        reportAsExternal("bulkWrite");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<BulkWriteResult> bulkWrite(final ClientSession clientSession,
                                                final List<? extends WriteModel<? extends TDocument>> requests,
                                                final BulkWriteOptions options) {
        reportAsExternal("bulkWrite");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Success> insertOne(final TDocument document, final InsertOneOptions options) {
        reportAsExternal("insertOne");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<Success> insertOne(final ClientSession clientSession, final TDocument document,
                                        final InsertOneOptions options) {
        reportAsExternal("insertOne");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<Success> insertMany(final List<? extends TDocument> documents, final InsertManyOptions options) {
        reportAsExternal("insertMany");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Success> insertMany(final ClientSession clientSession, final List<? extends TDocument> documents,
                                         final InsertManyOptions options) {
        reportAsExternal("insertMany");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteOne(final Bson filter, final DeleteOptions options) {
        reportAsExternal("deleteOne");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteOne(final ClientSession clientSession, final Bson filter,
                                             final DeleteOptions options) {
        reportAsExternal("deleteOne");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteMany(final Bson filter, final DeleteOptions options) {

        reportAsExternal("deleteMany");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteMany(final ClientSession clientSession, final Bson filter,
                                              final DeleteOptions options) {

        reportAsExternal("deleteMany");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> replaceOne(final Bson filter, final TDocument replacement,
                                              final ReplaceOptions options) {
        reportAsExternal("replaceOne");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> replaceOne(final ClientSession clientSession, final Bson filter,
                                              final TDocument replacement,
                                              final ReplaceOptions options) {

        reportAsExternal("replaceOne");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final Bson filter, final Bson update, final UpdateOptions options) {

        reportAsExternal("updateOne");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final ClientSession clientSession, final Bson filter, final Bson update,
                                             final UpdateOptions options) {
        reportAsExternal("updateOne");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final Bson filter, final List<? extends Bson> update,
                                             final UpdateOptions options) {
        reportAsExternal("updateOne");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final ClientSession clientSession, final Bson filter,
                                             final List<? extends Bson> update,
                                             final UpdateOptions options) {

        reportAsExternal("updateOne");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final Bson filter, final Bson update, final UpdateOptions options) {

        reportAsExternal("updateMany");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final ClientSession clientSession, final Bson filter, final Bson update,
                                              final UpdateOptions options) {
        reportAsExternal("updateMany");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final Bson filter, final List<? extends Bson> update,
                                              final UpdateOptions options) {

        reportAsExternal("updateMany");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final ClientSession clientSession, final Bson filter,
                                              final List<? extends Bson> update,
                                              final UpdateOptions options) {
        reportAsExternal("updateMany");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndDelete(final Bson filter, final FindOneAndDeleteOptions options) {

        reportAsExternal("findOneAndDelete");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndDelete(final ClientSession clientSession, final Bson filter,
                                                 final FindOneAndDeleteOptions options) {
        reportAsExternal("findOneAndDelete");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndReplace(final Bson filter, final TDocument replacement,
                                                  final FindOneAndReplaceOptions options) {
        reportAsExternal("findOneAndReplace");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndReplace(final ClientSession clientSession, final Bson filter,
                                                  final TDocument replacement,
                                                  final FindOneAndReplaceOptions options) {

        reportAsExternal("findOneAndReplace");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndUpdate(final Bson filter, final Bson update,
                                                 final FindOneAndUpdateOptions options) {

        reportAsExternal("findOneAndUpdate");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndUpdate(final ClientSession clientSession, final Bson filter,
                                                 final Bson update,
                                                 final FindOneAndUpdateOptions options) {
        reportAsExternal("findOneAndUpdate");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndUpdate(final Bson filter, final List<? extends Bson> update,
                                                 final FindOneAndUpdateOptions options) {
        reportAsExternal("findOneAndUpdate");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndUpdate(final ClientSession clientSession, final Bson filter,
                                                 final List<? extends Bson> update,
                                                 final FindOneAndUpdateOptions options) {

        reportAsExternal("findOneAndUpdate");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<Success> drop() {

        reportAsExternal("drop");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<Success> drop(final ClientSession clientSession) {
        reportAsExternal("drop");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<String> createIndex(final Bson key, final IndexOptions options) {
        reportAsExternal("createIndex");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<String> createIndex(final ClientSession clientSession, final Bson key,
                                         final IndexOptions options) {
        reportAsExternal("createIndex");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<String> createIndexes(final List<IndexModel> indexes,
                                           final CreateIndexOptions createIndexOptions) {
        reportAsExternal("createIndexes");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<String> createIndexes(final ClientSession clientSession, final List<IndexModel> indexes,
                                           final CreateIndexOptions createIndexOptions) {
        reportAsExternal("createIndexes");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> ListIndexesPublisher<TResult> listIndexes(final Class<TResult> clazz) {

        reportAsExternal("listIndexes");
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <TResult> ListIndexesPublisher<TResult> listIndexes(final ClientSession clientSession,
                                                               final Class<TResult> clazz) {
        reportAsExternal("listIndexes");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Success> dropIndex(final String indexName, final DropIndexOptions dropIndexOptions) {
        reportAsExternal("dropIndex");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Success> dropIndex(final Bson keys, final DropIndexOptions dropIndexOptions) {
        reportAsExternal("dropIndex");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Success> dropIndex(final ClientSession clientSession, final String indexName,
                                        final DropIndexOptions dropIndexOptions) {
        reportAsExternal("dropIndex");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Success> dropIndex(final ClientSession clientSession, final Bson keys,
                                        final DropIndexOptions dropIndexOptions) {
        reportAsExternal("dropIndex");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Success> renameCollection(final MongoNamespace newCollectionNamespace,
                                               final RenameCollectionOptions options) {
        reportAsExternal("renameCollection");
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Success> renameCollection(final ClientSession clientSession,
                                               final MongoNamespace newCollectionNamespace,
                                               final RenameCollectionOptions options) {
        reportAsExternal("renameCollection");
        return Weaver.callOriginal();

    }

    private void reportAsExternal(String operation) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        if (transaction != null) {
            DatastoreParameters params = DatastoreParameters
                    .product(DatastoreVendor.MongoDB.name())
                    .collection(getNamespace().getCollectionName())
                    .operation(operation)
                    .noInstance()
                    // TODO: get the DB instance host:port
                    //.instance()
                    .databaseName(getNamespace().getDatabaseName())
                    .build();
            transaction.getTracedMethod().reportAsExternal(params);
        }
    }

}
