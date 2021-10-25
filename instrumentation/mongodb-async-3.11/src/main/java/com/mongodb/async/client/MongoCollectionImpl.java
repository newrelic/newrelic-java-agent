package com.mongodb.async.client;

import java.util.List;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
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
abstract class MongoCollectionImpl<TDocument>  implements MongoCollection<TDocument> {

	@NewField
	public ServerAddress address = new ServerAddress("unknown");

	public abstract MongoNamespace getNamespace();

	@WeaveAllConstructors
	MongoCollectionImpl() {

	}

	@Trace
	public void count(Bson filter, CountOptions options,SingleResultCallback<Long> callback) {
		callback = instrument(callback, MongoUtil.OP_COUNT);
		Weaver.callOriginal();
	}


	@Trace
	public <TResult> DistinctIterable<TResult> distinct(final String fieldName, final Bson filter, final Class<TResult> resultClass)  {
		return Weaver.callOriginal();
	}

	@Trace
	public <TResult> FindIterable<TResult> find(Bson filter, Class<TResult> resultClass) {
		return Weaver.callOriginal();
	}

	@Trace
	public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> pipeline, Class<TResult> resultClass) {
		return Weaver.callOriginal();
	}

	@Trace
	public <TResult> MapReduceIterable<TResult> mapReduce(String mapFunction, String reduceFunction, Class<TResult> resultClass) {
		return Weaver.callOriginal();
	}

	/*
	 * This eventually calls MixedBulkWriteOperation, which is where we record metrics for each individual operation.
	 * Hence no leaf = true here.
	 */
	@Trace
	public void bulkWrite(List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options, SingleResultCallback<BulkWriteResult> callback) {
		callback = instrument(callback, MongoUtil.OP_BULK_WRITE);
		Weaver.callOriginal();
	}

	@Trace
	public void insertOne(TDocument document, InsertOneOptions options, SingleResultCallback<Void> callback)  {
		callback = instrument(callback, MongoUtil.OP_INSERT);
		Weaver.callOriginal();
	}

	@Trace
	public void insertMany(List<? extends TDocument> documents, InsertManyOptions options, SingleResultCallback<Void> callback) {

		callback = instrument(callback, MongoUtil.OP_INSERT_MANY);
		Weaver.callOriginal();
	}

	@Trace
	public void deleteOne(Bson filter, DeleteOptions options, SingleResultCallback<DeleteResult> callback)  {
		callback = instrument(callback, MongoUtil.OP_DELETE);
		Weaver.callOriginal();
	}

	@Trace
	public void deleteMany(Bson filter, DeleteOptions options, SingleResultCallback<DeleteResult> callback) {
		callback = instrument(callback, MongoUtil.OP_DELETE);
		Weaver.callOriginal();
	}

	@Trace
	public void replaceOne(Bson filter, TDocument replacement, UpdateOptions options, SingleResultCallback<UpdateResult> callback) {
		callback = instrument(callback, MongoUtil.OP_REPLACE);
		Weaver.callOriginal();
	}

	@Trace
	public void updateOne(Bson filter, Bson update, UpdateOptions options, SingleResultCallback<UpdateResult> callback) {
		callback = instrument(callback, MongoUtil.OP_UPDATE);
		Weaver.callOriginal();
	}

	@Trace
	public void updateMany(Bson filter, Bson update, UpdateOptions options, SingleResultCallback<UpdateResult> callback) {
		callback = instrument(callback, MongoUtil.OP_UPDATE_MANY);
		Weaver.callOriginal();
	}

	@Trace
	public void findOneAndDelete(Bson filter, FindOneAndDeleteOptions options, SingleResultCallback<TDocument> callback) {
		callback = instrument(callback, MongoUtil.OP_FIND_AND_DELETE);
		Weaver.callOriginal();
	}

	@Trace
	public void findOneAndReplace(Bson filter, TDocument replacement, FindOneAndReplaceOptions options, SingleResultCallback<TDocument> callback) {
		callback = instrument(callback, MongoUtil.OP_FIND_AND_REPLACE);
		Weaver.callOriginal();
	}

	@Trace
	public void findOneAndUpdate(Bson filter, Bson update, FindOneAndUpdateOptions options, SingleResultCallback<TDocument> callback) {
		callback = instrument(callback, MongoUtil.OP_FIND_AND_UPDATE);
		Weaver.callOriginal();
	}

	@Trace
	public void drop(SingleResultCallback<Void> callback) {
		callback = instrument(callback, MongoUtil.OP_DROP_COLLECTION);
		Weaver.callOriginal();
	}

	@Trace
	public void createIndexes(List<IndexModel> indexes, SingleResultCallback<List<String>> callback) {
		callback = instrument(callback, MongoUtil.OP_REPLACE);
		Weaver.callOriginal();
	}

	public void createIndex(Bson key, IndexOptions indexOptions, SingleResultCallback<String> callback)  {
		callback = instrument(callback, MongoUtil.OP_REPLACE);
		Weaver.callOriginal();
	}

	@Trace
	public <TResult> ListIndexesIterable<TResult> listIndexes(Class<TResult> resultClass) {
		return Weaver.callOriginal();
	}

	@Trace
	public void dropIndex(String indexName,SingleResultCallback<Void> callback) {
		callback = instrument(callback, MongoUtil.OP_DROP_INDEX);
		Weaver.callOriginal();
	}

	@Trace
	public void dropIndex(Bson keys,SingleResultCallback<Void> callback) {
		callback = instrument(callback, MongoUtil.OP_DROP_INDEX);
		Weaver.callOriginal();
	}

	@Trace
	public void renameCollection(MongoNamespace newCollectionNamespace, RenameCollectionOptions options, SingleResultCallback<Void> callback) {
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


	public MongoCollection<TDocument> withCodecRegistry(CodecRegistry codecRegistry) {
		MongoCollectionImpl<TDocument> collection = Weaver.callOriginal();
		collection.address = address;
		return collection;
	}

	public <NewTDocument> MongoCollection<NewTDocument> withDocumentClass(Class<NewTDocument> newDocumentClass) {
		MongoCollectionImpl<NewTDocument> collection = Weaver.callOriginal();
		collection.address = address;
		return collection;
	}

	public MongoCollection<TDocument> withReadConcern(ReadConcern readConcern) {
		MongoCollectionImpl<TDocument> collection = Weaver.callOriginal();
		collection.address = address;
		return collection;
	}

	public MongoCollection<TDocument> withWriteConcern(WriteConcern writeConcern)  {
		MongoCollectionImpl<TDocument> collection = Weaver.callOriginal();
		collection.address = address;
		return collection;
	}

	@Trace
	public abstract FindIterable<TDocument> find();

	@Trace
	public abstract FindIterable<TDocument> find(Bson filter);


}
