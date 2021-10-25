package com.mongodb.async.client;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.lang.Nullable;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.mongo.MongoUtil;

@Weave
abstract class FindIterableImpl<TDocument, TResult> extends MongoIterableImpl<TResult> {
	
	FindIterableImpl(@Nullable final ClientSession clientSession, final MongoNamespace namespace, final Class<TDocument> documentClass,
            final Class<TResult> resultClass, final CodecRegistry codecRegistry, final ReadPreference readPreference,
            final ReadConcern readConcern, final OperationExecutor executor, final Bson filter, final boolean retryReads)  {
		super(clientSession, executor, readConcern, readPreference, retryReads);
		super.operationName = MongoUtil.OP_FIND;
		super.collectionName = namespace.getCollectionName();
		super.databaseName = namespace.getDatabaseName();
	}
}
