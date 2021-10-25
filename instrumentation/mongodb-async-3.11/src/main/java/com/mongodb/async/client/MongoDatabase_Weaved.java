package com.mongodb.async.client;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type=MatchType.Interface,originalName="com.mongodb.async.client.MongoDatabase")
public abstract class MongoDatabase_Weaved {

    @NewField
    public ServerAddress address = new ServerAddress("unknown");

    public MongoCollection<Document> getCollection(final String collectionName) { 
        MongoCollectionImpl<Document> collection = Weaver.callOriginal();
        
        collection.address = address;
        return collection;
    	
    }

    public MongoDatabase_Weaved withCodecRegistry(final CodecRegistry codecRegistry) {
        MongoDatabase_Weaved database = Weaver.callOriginal();
        database.address = address;
        return database;
    }

    public MongoDatabase_Weaved withReadPreference(final ReadPreference readPreference) {
        MongoDatabase_Weaved database = Weaver.callOriginal();
        database.address = address;
        return database;
    }

    public MongoDatabase_Weaved withWriteConcern(final WriteConcern writeConcern) {
        MongoDatabase_Weaved database = Weaver.callOriginal();
        database.address = address;
        return database;
    }
}
