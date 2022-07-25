/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.internal.async.client;

import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;

@Weave(type = MatchType.Interface, originalName = "com/mongodb/internal/async/client/AsyncMongoDatabase")
public abstract class AsyncMongoDatabase_Instrumentation {

    @NewField
    public ServerAddress address = new ServerAddress(MongoUtil.UNKNOWN);

    public AsyncMongoCollection<Document> getCollection(final String collectionName) {
        AsyncMongoCollectionImpl_Instrumentation<Document> collection = Weaver.callOriginal();
        collection.address = address;
        return collection;
    }

    public AsyncMongoDatabase_Instrumentation withCodecRegistry(final CodecRegistry codecRegistry) {
        AsyncMongoDatabase_Instrumentation database = Weaver.callOriginal();
        database.address = address;
        return database;
    }

    public AsyncMongoDatabase_Instrumentation withReadPreference(final ReadPreference readPreference) {
        AsyncMongoDatabase_Instrumentation database = Weaver.callOriginal();
        database.address = address;
        return database;
    }

    public AsyncMongoDatabase_Instrumentation withWriteConcern(final WriteConcern writeConcern) {
        AsyncMongoDatabase_Instrumentation database = Weaver.callOriginal();
        database.address = address;
        return database;
    }
}
