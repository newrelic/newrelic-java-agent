/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.async.client;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.lang.Nullable;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.mongo.MongoUtil;
import org.bson.codecs.configuration.CodecRegistry;

@Weave(type = MatchType.ExactClass, originalName = "com/mongodb/async/client/ListDatabasesIterableImpl")
abstract class ListDatabasesIterableImpl_Instrumentation<TResult> extends MongoIterableImpl_Instrumentation<TResult> {

    ListDatabasesIterableImpl_Instrumentation(@Nullable final ClientSession clientSession, final Class<TResult> resultClass,
            final CodecRegistry codecRegistry, final ReadPreference readPreference,
            final OperationExecutor executor, final boolean retryReads) {
        super(clientSession, executor, ReadConcern.DEFAULT, readPreference, retryReads);
        super.collectionName = "allDatabases";
        super.databaseName = null;
        super.operationName = MongoUtil.OP_LIST_DATABASES;
    }
}
