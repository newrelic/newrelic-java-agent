package com.mongodb.internal.async.client;

import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.lang.Nullable;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.mongo.MongoUtil;

@Weave
abstract class AsyncMapReduceIterableImpl<TDocument, TResult> extends AsyncMongoIterableImpl<TResult> {
	
	AsyncMapReduceIterableImpl(@Nullable final AsyncClientSession clientSession, final MongoNamespace namespace,
            final Class<TDocument> documentClass, final Class<TResult> resultClass, final CodecRegistry codecRegistry,
            final ReadPreference readPreference, final ReadConcern readConcern, final WriteConcern writeConcern,
            final OperationExecutor executor, final String mapFunction, final String reduceFunction) {
		super(clientSession, executor, readConcern, readPreference,false);
		super.collectionName = namespace.getCollectionName();
		super.databaseName = namespace.getDatabaseName();
		super.operationName = MongoUtil.OP_MAP_REDUCE;
	}

}
