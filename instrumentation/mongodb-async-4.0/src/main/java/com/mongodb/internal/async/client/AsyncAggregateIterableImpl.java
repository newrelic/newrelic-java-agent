package com.mongodb.internal.async.client;

import java.util.List;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.internal.client.model.AggregationLevel;
import com.mongodb.lang.Nullable;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.mongo.MongoUtil;

@Weave
abstract class AsyncAggregateIterableImpl<TDocument, TResult> extends AsyncMongoIterableImpl<TResult> {
		
	 AsyncAggregateIterableImpl(@Nullable final AsyncClientSession clientSession, final String databaseName,
             final Class<TDocument> documentClass, final Class<TResult> resultClass, final CodecRegistry codecRegistry,
             final ReadPreference readPreference, final ReadConcern readConcern, final WriteConcern writeConcern,
             final OperationExecutor executor, final List<? extends Bson> pipeline,
             final AggregationLevel aggregationLevel, final boolean retryReads) {
		super(clientSession, executor, readConcern, readPreference, retryReads);
		super.collectionName = "";
		super.databaseName = databaseName;
		super.operationName = MongoUtil.OP_AGGREGATE;
	}
}
