package com.mongodb.internal.async.client;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type=MatchType.Interface)
public abstract class AsyncMongoDatabase {

    @NewField
    public ServerAddress address = new ServerAddress("unknown");

    public AsyncMongoCollection<Document> getCollection(final String collectionName) { 
        AsyncMongoCollectionImpl<Document> collection = Weaver.callOriginal();
        
        collection.address = address;
        return collection;
    	
    }

    public AsyncMongoDatabase withCodecRegistry(final CodecRegistry codecRegistry) {
        AsyncMongoDatabase database = Weaver.callOriginal();
        database.address = address;
        return database;
    }

    public AsyncMongoDatabase withReadPreference(final ReadPreference readPreference) {
        AsyncMongoDatabase database = Weaver.callOriginal();
        database.address = address;
        return database;
    }

    public AsyncMongoDatabase withWriteConcern(final WriteConcern writeConcern) {
        AsyncMongoDatabase database = Weaver.callOriginal();
        database.address = address;
        return database;
    }
}
