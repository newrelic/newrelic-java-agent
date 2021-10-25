package com.mongodb.internal.async.client;

import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.lang.Nullable;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.mongo.MongoUtil;

@Weave
abstract class AsyncListDatabasesIterableImpl<TResult> extends AsyncMongoIterableImpl<TResult>  {

	AsyncListDatabasesIterableImpl(@Nullable final AsyncClientSession clientSession, final Class<TResult> resultClass,
            final CodecRegistry codecRegistry, final ReadPreference readPreference,
            final OperationExecutor executor, final boolean retryReads) {
		super(clientSession, executor, ReadConcern.DEFAULT, readPreference, retryReads);
		super.collectionName = "allDatabases";
		super.databaseName = null;
		super.operationName = MongoUtil.OP_LIST_DATABASES;
	}
}
