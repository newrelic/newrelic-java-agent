/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.async.client;

import com.newrelic.api.agent.weaver.MatchType;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.lang.Nullable;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.mongo.MongoUtil;

@Weave(type = MatchType.ExactClass, originalName = "com/mongodb/async/client/FindIterableImpl")
abstract class FindIterableImpl_Instrumentation<TDocument, TResult> extends MongoIterableImpl_Instrumentation<TResult> {
	
	FindIterableImpl_Instrumentation(@Nullable final ClientSession clientSession, final MongoNamespace namespace, final Class<TDocument> documentClass,
            final Class<TResult> resultClass, final CodecRegistry codecRegistry, final ReadPreference readPreference,
            final ReadConcern readConcern, final OperationExecutor executor, final Bson filter) {
		super(clientSession, executor, readConcern, readPreference);
		super.operationName = MongoUtil.OP_FIND;
		super.collectionName = namespace.getCollectionName();
		super.databaseName = namespace.getDatabaseName();
	}
}
