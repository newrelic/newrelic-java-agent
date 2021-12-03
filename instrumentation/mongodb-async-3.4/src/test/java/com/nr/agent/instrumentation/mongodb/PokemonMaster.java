///*
// *
// *  * Copyright 2020 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package com.nr.agent.instrumentation.mongodb;
//
//import com.mongodb.async.client.FindIterable;
//import com.mongodb.async.client.MongoClient;
//import com.mongodb.async.client.MongoCollection;
//import com.mongodb.async.client.MongoDatabase;
//import com.mongodb.client.result.DeleteResult;
//import com.newrelic.api.agent.Trace;
//import org.bson.Document;
//
//import java.net.UnknownHostException;
//import java.util.ArrayList;
//
//import static com.mongodb.client.model.Filters.lte;
//
///**
// * Demo app for managing pokemon with mongo... don't judge me.
// */
//public class PokemonMaster {
//    // connect to the local database server
//    private final MongoClient mongoClient;
//    private String dbName = "pokemon";
//    private String collectionName = "pokemon";
//
//    public PokemonMaster(MongoClient client) throws UnknownHostException {
//        mongoClient = client;
//    }
//
//    @Trace
//    public ArrayList<Document> demoFind() throws InterruptedException {
//        ArrayList<Document> orig = new ArrayList<>();
//        MongoDatabase db = mongoClient.getDatabase(dbName);
//        MongoCollection<Document> coll = db.getCollection(collectionName);
//
//        BasicDBObject query = new BasicDBObject("number", new BasicDBObject("$lte", 150).append("$gt", 0));
//
//        FindIterable<Document> cursor = coll.find(lte("number", 150));
//
//        cursor.forEach();
//        for (Document document : cursor) {
//            orig.add(document);
//        }
//        return orig;
//    }
//
//    @Trace
//    public FindIterable demoFindOne(String type) throws InterruptedException {
//        MongoDatabase db = mongoClient.getDatabase(dbName);
//        MongoCollection coll = db.getCollection(collectionName);
//        BasicDBObject query = new BasicDBObject("type", type);
//        return coll.find(query);
//    }
//
//    @Trace
//    public void demoInsert(Document document) throws InterruptedException {
//        MongoDatabase db = mongoClient.getDatabase(dbName);
//        MongoCollection coll = db.getCollection(collectionName);
//        coll.insertOne(document);
//    }
//
//    @Trace
//    public DeleteResult demoRemove(Document object) throws InterruptedException {
//        MongoDatabase db = mongoClient.getDatabase(dbName);
//        MongoCollection coll = db.getCollection(collectionName);
//        return coll.deleteOne(object);
//    }
//
//    @Trace
//    public Object demoUpdate(Document find, Document update) throws InterruptedException {
//        MongoDatabase db = mongoClient.getDatabase(dbName);
//        MongoCollection coll = db.getCollection(collectionName);
//        return coll.updateOne(find, update);
//    }
//
//    @Trace
//    public void demoError() throws InterruptedException {
//        MongoDatabase db = mongoClient.getDatabase(dbName);
//        MongoCollection<Document> coll = db.getCollection(collectionName);
//        Document query = new Document("number", 26);
//        Document result = coll.find(query).first();
//        coll.insertOne(result);
//    }
//}
