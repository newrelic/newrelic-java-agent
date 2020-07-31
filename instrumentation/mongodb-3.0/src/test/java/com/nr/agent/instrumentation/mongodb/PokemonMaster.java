/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mongodb;

import com.mongodb.AggregationOptions;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteResult;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ParallelScanOptions;
import com.mongodb.WriteResult;
import com.newrelic.api.agent.Trace;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Demo app for managing pokemon with mongo... don't judge me.
 */
public class PokemonMaster {
    // connect to the local database server
    private final MongoClient mongoClient;
    private String dbName = "pokemon";
    private String collectionName = "pokemon";

    public PokemonMaster(MongoClient client) throws UnknownHostException {
        mongoClient = client;
    }

    @Trace
    public ArrayList<DBObject> demoFind() throws InterruptedException {
        ArrayList<DBObject> orig = new ArrayList<>();
        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);

        BasicDBObject query = new BasicDBObject("number", new BasicDBObject("$lte", 150).append("$gt", 0));
        DBCursor cursor = coll.find(query);
        while (cursor.hasNext()) {
            orig.add(cursor.next());
        }
        cursor.close();
        return orig;
    }

    @Trace
    public DBObject demoFindOne(String type) throws InterruptedException {
        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);
        BasicDBObject query = new BasicDBObject("type", type);
        return coll.findOne(query);
    }

    @Trace
    public WriteResult demoInsert(DBObject object) throws InterruptedException {
        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);
        return coll.insert(object);
    }

    @Trace
    public WriteResult demoRemove(DBObject object) throws InterruptedException {
        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);
        return coll.remove(object);
    }

    @Trace
    public WriteResult demoUpdate(DBObject find, DBObject update) throws InterruptedException {
        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);
        return coll.update(find, update);
    }

    @Trace
    public WriteResult demoError() throws InterruptedException {
        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);
        DBObject query = new BasicDBObject("number", 26);
        DBObject result = coll.findOne(query);
        return coll.insert(result);
    }

    @Trace
    public BulkWriteResult demoBulkOperationOrdered() throws InterruptedException {

        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);
        BulkWriteOperation builder = coll.initializeOrderedBulkOperation();
        builder.find(new BasicDBObject()).removeOne();
        builder.find(new BasicDBObject("type", "psychic")).updateOne(new BasicDBObject("$set", new BasicDBObject(
                "nickname", "John Edward")));
        builder.insert(new BasicDBObject("name", "Pikachu").append("number", 25));
        return builder.execute();
    }

    @Trace
    public BulkWriteResult demoBulkOperationUnordered() throws InterruptedException {

        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);
        BulkWriteOperation builder = coll.initializeUnorderedBulkOperation();
        builder.insert(new BasicDBObject("name", "Missingno").append("number", null));
        builder.find(new BasicDBObject()).removeOne();
        builder.find(new BasicDBObject("type", "dragon")).updateOne(new BasicDBObject("$set", new BasicDBObject(
                "nickname", "Puff")));
        return builder.execute();
    }

    @Trace
    public ArrayList<DBObject> demoParallelScan() throws InterruptedException {
        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);

        ParallelScanOptions parallelScanOptions = ParallelScanOptions.builder().numCursors(3).batchSize(
                300).build();

        ArrayList<DBObject> results = new ArrayList<>();
        List<Cursor> cursors = coll.parallelScan(parallelScanOptions);
        for (Cursor pCursor : cursors) {
            while (pCursor.hasNext()) {
                results.add(pCursor.next());
            }
        }
        return results;
    }

    private List<DBObject> getAggregationPipeline() throws InterruptedException {

        // Group pokemon by type and sort by average attack of the group

        // create our pipeline operations, first with the $match
        DBObject match = new BasicDBObject("$match", new BasicDBObject("number", new BasicDBObject("$lte", 150)));

        // build the $projection operation
        DBObject fields = new BasicDBObject("type", 1);
        fields.put("attack", 1);
        fields.put("defense", 1);
        DBObject project = new BasicDBObject("$project", fields);

        // Now the $group operation
        DBObject groupFields = new BasicDBObject("_id", "$type");
        groupFields.put("AverageAttack", new BasicDBObject("$avg", "$attack"));
        DBObject group = new BasicDBObject("$group", groupFields);

        // Finally the $sort operation
        DBObject sort = new BasicDBObject("$sort", new BasicDBObject("AverageAttack", -1));

        // run aggregation
        List<DBObject> pipeline = Arrays.asList(match, project, group, sort);
        return pipeline;
    }

    @Trace
    public ArrayList<DBObject> demoAggregationResults() throws InterruptedException {
        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);
        List<DBObject> pipeline = getAggregationPipeline();

        AggregationOutput output = coll.aggregate(pipeline);
        ArrayList<DBObject> results = new ArrayList<>();
        // Output the results
        for (DBObject result : output.results()) {
            results.add(result);
        }
        return results;
    }

    @Trace
    public ArrayList<DBObject> demoAggregationCursor() throws InterruptedException {
        DB db = mongoClient.getDB(dbName);
        DBCollection coll = db.getCollection(collectionName);
        List<DBObject> pipeline = getAggregationPipeline();

        AggregationOptions aggregationOptions = AggregationOptions.builder().batchSize(100).outputMode(
                AggregationOptions.OutputMode.CURSOR).allowDiskUse(true).build();

        Cursor cursor = coll.aggregate(pipeline, aggregationOptions);
        ArrayList<DBObject> results = new ArrayList<>();
        while (cursor.hasNext()) {
            results.add(cursor.next());
        }
        return results;
    }
}
