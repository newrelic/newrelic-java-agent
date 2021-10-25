package com.mongodb.async.client;

import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.lang.Nullable;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.mongo.MongoUtil;

@Weave
abstract class ListIndexesIterableImpl<TResult> extends MongoIterableImpl<TResult>  {


	ListIndexesIterableImpl(@Nullable final ClientSession clientSession, final MongoNamespace namespace, final Class<TResult> resultClass,
            final CodecRegistry codecRegistry, final ReadPreference readPreference, final OperationExecutor executor,
            final boolean retryReads) {
		super(clientSession, executor, ReadConcern.DEFAULT, readPreference, retryReads);
		super.operationName = MongoUtil.OP_LIST_INDEX;
		super.collectionName = namespace.getCollectionName();
		super.databaseName = namespace.getDatabaseName();
	}
}
