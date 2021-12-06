/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.async.client;

import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;

@Weave(type = MatchType.Interface, originalName = "com/mongodb/async/client/MongoDatabase")
public abstract class MongoDatabase_Instrumentation {

    @NewField
    public ServerAddress address = new ServerAddress("unknown");

    public MongoCollection<Document> getCollection(final String collectionName) {
        MongoCollectionImpl_Instrumentation<Document> collection = Weaver.callOriginal();

        collection.address = address;
        return collection;
    }

    public MongoDatabase_Instrumentation withCodecRegistry(final CodecRegistry codecRegistry) {
        MongoDatabase_Instrumentation database = Weaver.callOriginal();
        database.address = address;
        return database;
    }

    public MongoDatabase_Instrumentation withReadPreference(final ReadPreference readPreference) {
        MongoDatabase_Instrumentation database = Weaver.callOriginal();
        database.address = address;
        return database;
    }

    public MongoDatabase_Instrumentation withWriteConcern(final WriteConcern writeConcern) {
        MongoDatabase_Instrumentation database = Weaver.callOriginal();
        database.address = address;
        return database;
    }
}
