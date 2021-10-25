package com.mongodb.internal.async.client;

import java.util.List;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateIndexOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.DropIndexOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
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
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.client.model.CountStrategy;
import com.mongodb.lang.Nullable;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;
import com.nr.agent.mongo.NRCallbackWrapper;

@Weave
abstract class AsyncMongoCollectionImpl<TDocument>  implements AsyncMongoCollection<TDocument> {

	@NewField
	public ServerAddress address = new ServerAddress("unknown");

	public abstract MongoNamespace getNamespace();

	@WeaveAllConstructors
	AsyncMongoCollectionImpl() {

	}

	@Trace
	private void executeCount(AsyncClientSession clientSession, Bson filter, CountOptions options, CountStrategy countStrategy, SingleResultCallback<Long> callback)  {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","count");
		callback = instrument(callback, MongoUtil.OP_COUNT);
		Weaver.callOriginal();
	}


	@Trace
	private <TResult> AsyncDistinctIterable<TResult> createDistinctIterable(AsyncClientSession clientSession, String fieldName, Bson filter, Class<TResult> resultClass) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","distinct");
		return Weaver.callOriginal();
	}

	@Trace
	private <TResult> AsyncFindIterable<TResult> createFindIterable(AsyncClientSession clientSession, Bson filter, Class<TResult> resultClass)  {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","find");
		return Weaver.callOriginal();
	}

	@Trace
	private <TResult> AsyncAggregateIterable<TResult> createAggregateIterable(AsyncClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> resultClass) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","aggregate");
		return Weaver.callOriginal();
	}

	@Trace
	private <TResult> AsyncMapReduceIterable<TResult> createMapReduceIterable(AsyncClientSession clientSession, String mapFunction, String reduceFunction,Class<TResult> resultClass) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","mapReduce");
		return Weaver.callOriginal();
	}

	@Trace
	private void executeBulkWrite(AsyncClientSession clientSession, List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options, SingleResultCallback<BulkWriteResult> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","bulkWrite");
		callback = instrument(callback, MongoUtil.OP_BULK_WRITE);
		Weaver.callOriginal();
	}

	@Trace
	private void executeInsertOne(AsyncClientSession clientSession, TDocument document, InsertOneOptions options, SingleResultCallback<InsertOneResult> callback)  {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","insertOne");
		callback = instrument(callback, MongoUtil.OP_INSERT);
		Weaver.callOriginal();
	}

	@Trace
	private void executeInsertMany(AsyncClientSession clientSession, List<? extends TDocument> documents, InsertManyOptions options, SingleResultCallback<InsertManyResult> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","insertMany");
		callback = instrument(callback, MongoUtil.OP_INSERT_MANY);
		Weaver.callOriginal();
	}

	@Trace
	private void executeDelete(AsyncClientSession clientSession, Bson filter, DeleteOptions options,boolean multi, SingleResultCallback<DeleteResult> callback){
		if(multi) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","deleteMany");
			callback = instrument(callback, MongoUtil.OP_DELETE_MANY);
		} else {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","deleteOne");
			callback = instrument(callback, MongoUtil.OP_DELETE);
		}
		Weaver.callOriginal();
	}

	@Trace
	private void executeReplaceOne(AsyncClientSession clientSession, Bson filter, TDocument replacement, ReplaceOptions options, SingleResultCallback<UpdateResult> callback){
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","replace");
		callback = instrument(callback, MongoUtil.OP_REPLACE);
		Weaver.callOriginal();
	}

	@Trace
	private void executeUpdate(@Nullable AsyncClientSession clientSession, Bson filter, Bson update, UpdateOptions options, boolean multi, SingleResultCallback<UpdateResult> callback) {
		if(multi) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","updateMany");
			callback = instrument(callback, MongoUtil.OP_UPDATE_MANY);
		} else {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","updateOne");
			callback = instrument(callback, MongoUtil.OP_UPDATE);
		}
		Weaver.callOriginal();
	}

	@Trace
	private void executeUpdate(@Nullable AsyncClientSession clientSession, Bson filter, List<? extends Bson> update, UpdateOptions options, boolean multi, SingleResultCallback<UpdateResult> callback) {
		if(multi) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","updateMany");
			callback = instrument(callback, MongoUtil.OP_UPDATE_MANY);
		} else {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","updateOne");
			callback = instrument(callback, MongoUtil.OP_UPDATE);
		}
		Weaver.callOriginal();
	}

	@Trace
	private void executeFindOneAndDelete(AsyncClientSession clientSession, Bson filter, FindOneAndDeleteOptions options, SingleResultCallback<TDocument> callback)  {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","findAndDelete");
		callback = instrument(callback, MongoUtil.OP_FIND_AND_DELETE);
		Weaver.callOriginal();
	}

	@Trace
	private void executeFindOneAndReplace(AsyncClientSession clientSession, Bson filter, TDocument replacement, FindOneAndReplaceOptions options, SingleResultCallback<TDocument> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","findAndReplace");
		callback = instrument(callback, MongoUtil.OP_FIND_AND_REPLACE);
		Weaver.callOriginal();
	}

	@Trace
	private void executeFindOneAndUpdate(@Nullable AsyncClientSession clientSession, Bson filter, Bson update, FindOneAndUpdateOptions options, SingleResultCallback<TDocument> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","findAndUpdate");
		callback = instrument(callback, MongoUtil.OP_FIND_AND_UPDATE);
		Weaver.callOriginal();
	}

	@Trace
	private void executeFindOneAndUpdate(@Nullable AsyncClientSession clientSession, Bson filter, List<? extends Bson> update, FindOneAndUpdateOptions options, SingleResultCallback<TDocument> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","findAndUpdate");
		callback = instrument(callback, MongoUtil.OP_FIND_AND_UPDATE);
		Weaver.callOriginal();
	}

	@Trace
	private void executeDrop(AsyncClientSession clientSession, SingleResultCallback<Void> callback)  {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","drop");
		callback = instrument(callback, MongoUtil.OP_DROP_COLLECTION);
		Weaver.callOriginal();
	}

	@Trace
	private void executeCreateIndexes(AsyncClientSession clientSession, List<IndexModel> indexes, CreateIndexOptions createIndexOptions, SingleResultCallback<List<String>> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","createIndexes");
		callback = instrument(callback, MongoUtil.OP_CREATE_INDEXES);
		Weaver.callOriginal();
	}

	@Trace
	private <TResult> AsyncListIndexesIterable<TResult> createListIndexesIterable(AsyncClientSession clientSession, Class<TResult> resultClass) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","createListIndexes");
		return Weaver.callOriginal();
	}

	@Trace
	private void executeDropIndex(AsyncClientSession clientSession, String indexName, DropIndexOptions dropIndexOptions, SingleResultCallback<Void> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","dropIndex");
		callback = instrument(callback, MongoUtil.OP_DROP_INDEX);
		Weaver.callOriginal();
	}


	@Trace
	private void executeDropIndex(AsyncClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions, SingleResultCallback<Void> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","dropIndex");
		callback = instrument(callback, MongoUtil.OP_DROP_INDEX);
		Weaver.callOriginal();
	}

	@Trace
	private void executeRenameCollection(AsyncClientSession clientSession, MongoNamespace newCollectionNamespace, RenameCollectionOptions options, SingleResultCallback<Void> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","AsyncMongoCollection","renameCollection");
		callback = instrument(callback, MongoUtil.OP_RENAME_COLLECTION);
		Weaver.callOriginal();
	}

	private <T> SingleResultCallback<T> instrument(SingleResultCallback<T> callback, String operationName) {
		if(callback instanceof NRCallbackWrapper) {
			return callback;
		}
		NRCallbackWrapper<T> wrapper = new NRCallbackWrapper<T>(callback);

		DatastoreParameters params = DatastoreParameters
				.product(DatastoreVendor.MongoDB.name())
				.collection(getNamespace().getCollectionName())
				.operation(operationName)
				.instance(address.getHost(), address.getPort())
				.databaseName(getNamespace().getDatabaseName())
				.build();
		wrapper.params = params;
		wrapper.token = NewRelic.getAgent().getTransaction().getToken();
		wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
		return wrapper;
	}


	public AsyncMongoCollection<TDocument> withCodecRegistry(CodecRegistry codecRegistry) {
		AsyncMongoCollectionImpl<TDocument> collection = Weaver.callOriginal();
		collection.address = address;
		return collection;
	}

	public <NewTDocument> AsyncMongoCollection<NewTDocument> withDocumentClass(Class<NewTDocument> newDocumentClass) {
		AsyncMongoCollectionImpl<NewTDocument> collection = Weaver.callOriginal();
		collection.address = address;
		return collection;
	}

	public AsyncMongoCollection<TDocument> withReadConcern(ReadConcern readConcern) {
		AsyncMongoCollectionImpl<TDocument> collection = Weaver.callOriginal();
		collection.address = address;
		return collection;
	}

	public AsyncMongoCollection<TDocument> withWriteConcern(WriteConcern writeConcern)  {
		AsyncMongoCollectionImpl<TDocument> collection = Weaver.callOriginal();
		collection.address = address;
		return collection;
	}


}
