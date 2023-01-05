package com.mongodb.reactivestreams.client;

import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
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
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;

import java.util.List;

import static com.nr.agent.mongo.MongoUtil.OP_AGGREGATE;
import static com.nr.agent.mongo.MongoUtil.OP_BULK_WRITE;
import static com.nr.agent.mongo.MongoUtil.OP_CREATE_INDEX;
import static com.nr.agent.mongo.MongoUtil.OP_CREATE_INDEXES;
import static com.nr.agent.mongo.MongoUtil.OP_DELETE_MANY;
import static com.nr.agent.mongo.MongoUtil.OP_DELETE_ONE;
import static com.nr.agent.mongo.MongoUtil.OP_DROP_COLLECTION;
import static com.nr.agent.mongo.MongoUtil.OP_DROP_INDEX;
import static com.nr.agent.mongo.MongoUtil.OP_FIND;
import static com.nr.agent.mongo.MongoUtil.OP_FIND_ONE_AND_DELETE;
import static com.nr.agent.mongo.MongoUtil.OP_FIND_ONE_AND_REPLACE;
import static com.nr.agent.mongo.MongoUtil.OP_FIND_ONE_AND_UPDATE;
import static com.nr.agent.mongo.MongoUtil.OP_INSERT_MANY;
import static com.nr.agent.mongo.MongoUtil.OP_INSERT_ONE;
import static com.nr.agent.mongo.MongoUtil.OP_LIST_INDEXES;
import static com.nr.agent.mongo.MongoUtil.OP_MAP_REDUCE;
import static com.nr.agent.mongo.MongoUtil.OP_RENAME_COLLECTION;
import static com.nr.agent.mongo.MongoUtil.OP_REPLACE_ONE;
import static com.nr.agent.mongo.MongoUtil.OP_UPDATE_MANY;
import static com.nr.agent.mongo.MongoUtil.OP_UPDATE_ONE;
import static com.nr.agent.mongo.MongoUtil.UNKNOWN;

@Weave(type = MatchType.Interface, originalName = "com.mongodb.reactivestreams.client.MongoCollection")
public abstract class MongoCollection_Instrumentation<TDocument> {
    
    @NewField
    ServerAddress serverAddress = new ServerAddress(UNKNOWN);

    public MongoNamespace getNamespace() {
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <TResult> FindPublisher<TResult> find(final Bson filter, final Class<TResult> clazz) {
        reportAsExternal(OP_FIND);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> FindPublisher<TResult> find(final ClientSession clientSession, final Bson filter,
            final Class<TResult> clazz) {
        reportAsExternal(OP_FIND);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> AggregatePublisher<TResult> aggregate(final List<? extends Bson> pipeline,
            final Class<TResult> clazz) {
        reportAsExternal(OP_AGGREGATE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> AggregatePublisher<TResult> aggregate(final ClientSession clientSession,
            final List<? extends Bson> pipeline,
            final Class<TResult> clazz) {
        reportAsExternal(OP_AGGREGATE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> MapReducePublisher<TResult> mapReduce(final String mapFunction, final String reduceFunction,
            final Class<TResult> clazz) {
        reportAsExternal(OP_MAP_REDUCE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> MapReducePublisher<TResult> mapReduce(final ClientSession clientSession, final String mapFunction,
            final String reduceFunction, final Class<TResult> clazz) {

        reportAsExternal(OP_MAP_REDUCE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<BulkWriteResult> bulkWrite(final List<? extends WriteModel<? extends TDocument>> requests,
            final BulkWriteOptions options) {

        reportAsExternal(OP_BULK_WRITE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<BulkWriteResult> bulkWrite(final ClientSession clientSession,
            final List<? extends WriteModel<? extends TDocument>> requests,
            final BulkWriteOptions options) {
        reportAsExternal(OP_BULK_WRITE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<InsertOneResult> insertOne(final TDocument document, final InsertOneOptions options) {
        reportAsExternal(OP_INSERT_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<InsertOneResult> insertOne(final ClientSession clientSession, final TDocument document,
            final InsertOneOptions options) {
        reportAsExternal(OP_INSERT_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<InsertManyResult> insertMany(final List<? extends TDocument> documents, final InsertManyOptions options) {
        reportAsExternal(OP_INSERT_MANY);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<InsertManyResult> insertMany(final ClientSession clientSession, final List<? extends TDocument> documents,
            final InsertManyOptions options) {
        reportAsExternal(OP_INSERT_MANY);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteOne(final Bson filter, final DeleteOptions options) {
        reportAsExternal(OP_DELETE_ONE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteOne(final ClientSession clientSession, final Bson filter,
            final DeleteOptions options) {
        reportAsExternal(OP_DELETE_ONE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteMany(final Bson filter, final DeleteOptions options) {

        reportAsExternal(OP_DELETE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<DeleteResult> deleteMany(final ClientSession clientSession, final Bson filter,
            final DeleteOptions options) {

        reportAsExternal(OP_DELETE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> replaceOne(final Bson filter, final TDocument replacement,
            final ReplaceOptions options) {
        reportAsExternal(OP_REPLACE_ONE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> replaceOne(final ClientSession clientSession, final Bson filter,
            final TDocument replacement,
            final ReplaceOptions options) {

        reportAsExternal(OP_REPLACE_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final Bson filter, final Bson update, final UpdateOptions options) {

        reportAsExternal(OP_UPDATE_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final ClientSession clientSession, final Bson filter, final Bson update,
            final UpdateOptions options) {
        reportAsExternal(OP_UPDATE_ONE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final Bson filter, final List<? extends Bson> update,
            final UpdateOptions options) {
        reportAsExternal(OP_UPDATE_ONE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateOne(final ClientSession clientSession, final Bson filter,
            final List<? extends Bson> update,
            final UpdateOptions options) {

        reportAsExternal(OP_UPDATE_ONE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final Bson filter, final Bson update, final UpdateOptions options) {

        reportAsExternal(OP_UPDATE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final ClientSession clientSession, final Bson filter, final Bson update,
            final UpdateOptions options) {
        reportAsExternal(OP_UPDATE_MANY);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final Bson filter, final List<? extends Bson> update,
            final UpdateOptions options) {

        reportAsExternal(OP_UPDATE_MANY);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<UpdateResult> updateMany(final ClientSession clientSession, final Bson filter,
            final List<? extends Bson> update,
            final UpdateOptions options) {
        reportAsExternal(OP_UPDATE_MANY);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndDelete(final Bson filter, final FindOneAndDeleteOptions options) {

        reportAsExternal(OP_FIND_ONE_AND_DELETE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndDelete(final ClientSession clientSession, final Bson filter,
            final FindOneAndDeleteOptions options) {
        reportAsExternal(OP_FIND_ONE_AND_DELETE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndReplace(final Bson filter, final TDocument replacement,
            final FindOneAndReplaceOptions options) {
        reportAsExternal(OP_FIND_ONE_AND_REPLACE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndReplace(final ClientSession clientSession, final Bson filter,
            final TDocument replacement,
            final FindOneAndReplaceOptions options) {

        reportAsExternal(OP_FIND_ONE_AND_REPLACE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndUpdate(final Bson filter, final Bson update,
            final FindOneAndUpdateOptions options) {

        reportAsExternal(OP_FIND_ONE_AND_UPDATE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndUpdate(final ClientSession clientSession, final Bson filter,
            final Bson update,
            final FindOneAndUpdateOptions options) {
        reportAsExternal(OP_FIND_ONE_AND_UPDATE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndUpdate(final Bson filter, final List<? extends Bson> update,
            final FindOneAndUpdateOptions options) {
        reportAsExternal(OP_FIND_ONE_AND_UPDATE);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<TDocument> findOneAndUpdate(final ClientSession clientSession, final Bson filter,
            final List<? extends Bson> update,
            final FindOneAndUpdateOptions options) {

        reportAsExternal(OP_FIND_ONE_AND_UPDATE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<Void> drop() {

        reportAsExternal(OP_DROP_COLLECTION);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public Publisher<Void> drop(final ClientSession clientSession) {
        reportAsExternal(OP_DROP_COLLECTION);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<String> createIndex(final Bson key, final IndexOptions options) {
        reportAsExternal(OP_CREATE_INDEX);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<String> createIndex(final ClientSession clientSession, final Bson key,
            final IndexOptions options) {
        reportAsExternal(OP_CREATE_INDEX);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<String> createIndexes(final List<IndexModel> indexes,
            final CreateIndexOptions createIndexOptions) {
        reportAsExternal(OP_CREATE_INDEXES);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<String> createIndexes(final ClientSession clientSession, final List<IndexModel> indexes,
            final CreateIndexOptions createIndexOptions) {
        reportAsExternal(OP_CREATE_INDEXES);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public <TResult> ListIndexesPublisher<TResult> listIndexes(final Class<TResult> clazz) {

        reportAsExternal(OP_LIST_INDEXES );
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <TResult> ListIndexesPublisher<TResult> listIndexes(final ClientSession clientSession,
            final Class<TResult> clazz) {
        reportAsExternal(OP_LIST_INDEXES );
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Void> dropIndex(final String indexName, final DropIndexOptions dropIndexOptions) {
        reportAsExternal(OP_DROP_INDEX);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Void> dropIndex(final Bson keys, final DropIndexOptions dropIndexOptions) {
        reportAsExternal(OP_DROP_INDEX);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Void> dropIndex(final ClientSession clientSession, final String indexName,
            final DropIndexOptions dropIndexOptions) {
        reportAsExternal(OP_DROP_INDEX);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Void> dropIndex(final ClientSession clientSession, final Bson keys,
            final DropIndexOptions dropIndexOptions) {
        reportAsExternal(OP_DROP_INDEX);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Void> renameCollection(final MongoNamespace newCollectionNamespace,
            final RenameCollectionOptions options) {
        reportAsExternal(OP_RENAME_COLLECTION);
        return Weaver.callOriginal();

    }

    @Trace(leaf = true)
    public Publisher<Void> renameCollection(final ClientSession clientSession,
            final MongoNamespace newCollectionNamespace,
            final RenameCollectionOptions options) {
        reportAsExternal(OP_RENAME_COLLECTION);
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
                    .databaseName(getNamespace().getDatabaseName())
                    .build();
            transaction.getTracedMethod().reportAsExternal(params);
        }
    }

}
