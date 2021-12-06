/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mongodb;

import com.mongodb.Block;
import com.mongodb.async.AsyncBatchCursor;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

/**
 * Exercises core MongoDB Async APIs to test agent instrumentation
 * Adapted from https://mongodb.github.io/mongo-java-driver/3.4/driver-async/getting-started/quick-start/
 */
public class MongoDbAsyncQuickStart {
    private static final String DB_NAME = "mydb";
    private static final String FIELD_NAME = "i";
    private final MongoClient mongoClient;

    // Callbacks for MongoDb async API results
    SingleResultCallback<Document> callbackPrintDocuments = (document, t) -> {
        if (document == null) {
            System.out.println("The document was null");
        } else {
            System.out.println(document.toJson());
        }
    };
    SingleResultCallback<Void> callbackWhenDropFinished = (result, t) -> System.out.println("Drop operation finished!");
    SingleResultCallback<Void> callbackWhenInsertFinished = (result, t) -> System.out.println("Insert operation finished!");
    SingleResultCallback<Void> callbackWhenFindAllFinished = (result, t) -> System.out.println("Find all operation finished!");
    SingleResultCallback<Long> callbackCountFinished = (count, t) -> {
        if (count == null) {
            System.out.println("The count was null");
        } else {
            System.out.println("Total count of documents: " + count);
        }
    };
    SingleResultCallback<UpdateResult> callbackUpdateResult = (result, t) -> {
        if (result == null) {
            System.out.println("The result was null");
        } else {
            System.out.println("Updated count of documents: " + result.getModifiedCount());
        }
    };
    SingleResultCallback<DeleteResult> callbackDeleteResult = (result, t) -> {
        if (result == null) {
            System.out.println("The result was null");
        } else {
            System.out.println("Count of documents deleted: " + result.getDeletedCount());
        }
    };
    SingleResultCallback<AsyncBatchCursor<Document>> callbackAsyncBatchCursorResult = (result, t) -> {
        if (result == null) {
            System.out.println("The result was null");
        } else {
            System.out.println("AsyncBatchCursor batch size: " + result.getBatchSize());
        }
    };
    SingleResultCallback<BulkWriteResult> callbackBulkWriteResult = (result, t) -> {
        if (result == null) {
            System.out.println("The result was null");
        } else {
            int deletedCount = result.getDeletedCount();
            int insertedCount = result.getInsertedCount();
            int matchedCount = result.getMatchedCount();

            int modifiedCount = 0;
            if (result.isModifiedCountAvailable()) {
                modifiedCount = result.getModifiedCount();
            }

            System.out.println("Bulk write deleted count: " + deletedCount + ", modified count: " + modifiedCount
                    + ", inserted count: " + insertedCount + ", matched count: " + matchedCount);
        }
    };
    Block<Document> printDocumentBlock = document -> {
        if (document == null) {
            System.out.println("The document was null");
        } else {
            System.out.println(document.toJson());
        }
    };

    public MongoDbAsyncQuickStart(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    /**
     * Exercises a handful of the MongoDB async APIs
     */
    public void run() {
        // Setup database
        MongoDatabase mongoDatabase = accessDatabase(DB_NAME);
        MongoCollection<Document> mongoCollection = accessCollection(mongoDatabase);

        // Clear collection
        // Contributes 1 to value of Datastore/operation/MongoDB/drop
        dropCollection(mongoCollection);

        // Create a document and insert it
        // Contributes 1 to value of Datastore/operation/MongoDB/insert
        Document document = createDocument();
        insertOneDocument(mongoCollection, document);

        // Find first document and print it out (it should only one in there as we dropped the rest earlier on)
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        findFirstDocument(mongoCollection);

        // Add lots of little documents to the collection, so we can explore queries and cursors
        // Contributes 1 to value of Datastore/operation/MongoDB/insertMany
        List<Document> listOfDocuments = createListOfDocuments(FIELD_NAME, 100);
        insertManyDocuments(mongoCollection, listOfDocuments);
        countDocuments(mongoCollection);

        // Find first document and print it out
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        findFirstDocument(mongoCollection);

        // Get all the documents in the collection and print them out
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        findAllDocuments(mongoCollection);

        // Get a batch of documents in the collection and print them out
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        findBatchOfDocuments(mongoCollection, 10);

        // Get a batch of documents in the collection and print out the batch size
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        findBatchOfDocumentsAsyncCursor(mongoCollection, 5);

        // Get one document using an eq filter (value == 71) and print it out
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        findSingleDocumentSingleFilter(mongoCollection, FIELD_NAME, 71);

        // Get a range of documents using a gt filter (value > 50) and print them out
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        findRangeOfDocumentsSingleFilter(mongoCollection, FIELD_NAME, 50);

        // Get a range of documents using a gt and lte filter (value > 50 && <= 100) and print them out
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        findRangeOfDocumentsMultipleFilters(mongoCollection, FIELD_NAME, 50, 100);

        // Sort documents and print them out
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        sortDocuments(mongoCollection, FIELD_NAME);

        // Projection and print them out
        // Contributes 1 to value of Datastore/operation/MongoDB/find
        projectDocuments(mongoCollection);

        // Update one document and print out the new count
        // Contributes 1 to value of Datastore/operation/MongoDB/update
        updateOneDocument(mongoCollection, FIELD_NAME, 10, 110);

        // Update many documents and print out the new count
        // Contributes 1 to value of Datastore/operation/MongoDB/updateMany
        updateManyDocuments(mongoCollection, FIELD_NAME, 100, 100);

        // Delete one document and print out the count of how many were deleted
        // Contributes 1 to value of Datastore/operation/MongoDB/delete
        deleteOneDocument(mongoCollection, FIELD_NAME, 110);

        // Delete many documents and print out the count of how many were deleted
        // Contributes 1 to value of Datastore/operation/MongoDB/deleteMany
        deleteManyDocuments(mongoCollection, FIELD_NAME, 100);

        // Clear collection
        // Contributes 1 to value of Datastore/operation/MongoDB/drop
        dropCollection(mongoCollection);

        // Ordered bulk writes
        List<WriteModel<Document>> writes = createListOfWrites();
        // Contributes 1 to value of Datastore/operation/MongoDB/bulkWrite
        orderedBulkWrites(mongoCollection, writes);

        // Clear collection
        // Contributes 1 to value of Datastore/operation/MongoDB/drop
        dropCollection(mongoCollection);

        // Unordered bulk writes
        // Contributes 1 to value of Datastore/operation/MongoDB/bulkWrite
        unorderedBulkWrites(mongoCollection, writes);

        // Drop database to clean up
        // No metric generated by this operation
        dropDatabase(mongoDatabase);
    }

    /**
     * Access MongoDB database
     *
     * @return MongoDatabase
     */
    public MongoDatabase accessDatabase(String name) {
        return mongoClient.getDatabase(name);
    }

    /**
     * Access a MongoDB collection
     *
     * @return MongoCollection<Document>
     */
    public MongoCollection<Document> accessCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection("test");
    }

    /**
     * Drop MongoDB collection from database
     * Callback prints when the operation has finished.
     */
    public void dropCollection(MongoCollection<Document> mongoCollection) {
        mongoCollection.drop(callbackWhenDropFinished);
    }

    /**
     * Drop MongoDB database
     * Callback prints when the operation has finished.
     */
    public void dropDatabase(MongoDatabase mongoDatabase) {
        mongoDatabase.drop(callbackWhenDropFinished);
    }

    /**
     * Create a MongoDB Document
     *
     * @return Document
     */
    public Document createDocument() {
        return new Document("name", "MongoDB")
                .append("type", "database")
                .append("count", 1)
                .append("versions", Arrays.asList("v3.2", "v3.0", "v2.6"))
                .append("info", new Document("x", 203).append("y", 102));
    }

    /**
     * Insert one Document
     * Callback prints when the operation has finished.
     */
    public void insertOneDocument(MongoCollection<Document> mongoCollection, Document document) {
        mongoCollection.insertOne(document, callbackWhenInsertFinished);
    }

    /**
     * Insert many Documents
     * Callback prints when the operation has finished.
     */
    public void insertManyDocuments(MongoCollection<Document> mongoCollection, List<? extends Document> documents) {
        mongoCollection.insertMany(documents, callbackWhenInsertFinished);
    }

    /**
     * Find first Document in a Collection
     * Callback prints the contents of the Document when the operation has finished.
     */
    public void findFirstDocument(MongoCollection<Document> mongoCollection) {
        mongoCollection.find().first(callbackPrintDocuments);
    }

    /**
     * Find all Documents in a Collection
     * Block prints the contents of each Document.
     * Callback prints when the operation has finished.
     */
    public void findAllDocuments(MongoCollection<Document> mongoCollection) {
        mongoCollection.find().forEach(printDocumentBlock, callbackWhenFindAllFinished);
    }

    /**
     * Find all Documents in a Collection for a specific batch size
     * Block prints the contents of each Document.
     * Callback prints when the operation has finished.
     */
    public void findBatchOfDocuments(MongoCollection<Document> mongoCollection, int batchSize) {
        mongoCollection.find().batchSize(batchSize).forEach(printDocumentBlock, callbackWhenFindAllFinished);
    }

    /**
     * Find all Documents in a Collection for a specific batch size using AsyncBatchCursor
     * Callback prints the count of Documents in the batch when the operation has finished.
     */
    public void findBatchOfDocumentsAsyncCursor(MongoCollection<Document> mongoCollection, int batchSize) {
        mongoCollection.find().batchSize(batchSize).batchCursor(callbackAsyncBatchCursorResult);
    }

    /**
     * Find a single Document in a Collection using a single eq filter
     * Callback prints the contents of the Document when the operation has finished.
     */
    public void findSingleDocumentSingleFilter(MongoCollection<Document> mongoCollection, String fieldName, int value) {
        mongoCollection.find(eq(fieldName, value)).first(callbackPrintDocuments);
    }

    /**
     * Find a range of Documents in a Collection using a single gt filter
     * Block prints the contents of each Document.
     * Callback prints when the operation has finished.
     */
    public void findRangeOfDocumentsSingleFilter(MongoCollection<Document> mongoCollection, String fieldName, int value) {
        mongoCollection.find(gt(fieldName, value)).forEach(printDocumentBlock, callbackWhenFindAllFinished);
    }

    /**
     * Find a range of Documents in a Collection using both gt and lte filters
     * Block prints the contents of each Document.
     * Callback prints when the operation has finished.
     */
    public void findRangeOfDocumentsMultipleFilters(MongoCollection<Document> mongoCollection, String fieldName, int gtValue, int lteValue) {
        mongoCollection.find(and(gt(fieldName, gtValue), lte(fieldName, lteValue))).forEach(printDocumentBlock, callbackWhenFindAllFinished);
    }

    /**
     * Sort Documents in a Collection
     * Callback prints the contents of the Document when the operation has finished.
     */
    public void sortDocuments(MongoCollection<Document> mongoCollection, String fieldName) {
        mongoCollection.find(exists(fieldName)).sort(descending(fieldName)).first(callbackPrintDocuments);
    }

    /**
     * Projection of Documents in a Collection
     * Sets a document describing the fields to return for all matching documents.
     * Callback prints the contents of the Document when the operation has finished.
     */
    public void projectDocuments(MongoCollection<Document> mongoCollection) {
        mongoCollection.find().projection(excludeId()).first(callbackPrintDocuments);
    }

    /**
     * Count the Documents in a Collection
     * Callback prints the total count of Documents when the operation has finished.
     */
    public void countDocuments(MongoCollection<Document> mongoCollection) {
        mongoCollection.count(callbackCountFinished);
    }

    /**
     * Update one Document in a Collection
     * Callback prints the updated count of a Document when the operation has finished.
     */
    public void updateOneDocument(MongoCollection<Document> mongoCollection, String fieldName, int eqValue, int setValue) {
        mongoCollection.updateOne(eq(fieldName, eqValue), set(fieldName, setValue), callbackUpdateResult);
    }

    /**
     * Update many Documents in a Collection
     * Callback prints the updated counts of all Documents when the operation has finished.
     */
    public void updateManyDocuments(MongoCollection<Document> mongoCollection, String fieldName, int ltValue, int incValue) {
        mongoCollection.updateMany(lt(fieldName, ltValue), inc(fieldName, incValue), callbackUpdateResult);
    }

    /**
     * Delete one Document in a Collection
     * Callback prints the count of all deleted Documents when the operation has finished.
     */
    public void deleteOneDocument(MongoCollection<Document> mongoCollection, String fieldName, int value) {
        mongoCollection.deleteOne(eq(fieldName, value), callbackDeleteResult);
    }

    /**
     * Delete many Documents in a Collection
     * Callback prints the count of all deleted Documents when the operation has finished.
     */
    public void deleteManyDocuments(MongoCollection<Document> mongoCollection, String fieldName, int value) {
        mongoCollection.deleteMany(gte(fieldName, value), callbackDeleteResult);
    }

    /**
     * Ordered bulk write a list of Documents
     * Callback prints the count of all bulk operations when they have finished.
     */
    public void orderedBulkWrites(MongoCollection<Document> mongoCollection, List<WriteModel<Document>> writes) {
        mongoCollection.bulkWrite(writes, callbackBulkWriteResult);
    }

    /**
     * Unordered bulk write a list of Documents
     * Callback prints the count of all bulk operations when they have finished.
     */
    public void unorderedBulkWrites(MongoCollection<Document> mongoCollection, List<WriteModel<Document>> writes) {
        mongoCollection.bulkWrite(writes, new BulkWriteOptions().ordered(false), callbackBulkWriteResult);
    }

    /**
     * Add a configurable number of Documents to a Collection
     *
     * @param numOfDocuments number of Documents to add
     * @return List of Documents created
     */
    public List<Document> createListOfDocuments(String fieldName, int numOfDocuments) {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < numOfDocuments; i++) {
            documents.add(new Document(fieldName, i));
        }
        return documents;
    }

    /**
     * Create a list of writes for a bulkWrite operation
     *
     * @return list of WriteModel
     */
    public List<WriteModel<Document>> createListOfWrites() {
        List<WriteModel<Document>> writes = new ArrayList<>();
        writes.add(new InsertOneModel<>(new Document("_id", 4)));
        writes.add(new InsertOneModel<>(new Document("_id", 5)));
        writes.add(new InsertOneModel<>(new Document("_id", 6)));
        writes.add(new UpdateOneModel<>(new Document("_id", 1), new Document("$set", new Document("x", 2))));
        writes.add(new DeleteOneModel<>(new Document("_id", 2)));
        writes.add(new ReplaceOneModel<>(new Document("_id", 3), new Document("_id", 3).append("x", 4)));
        return writes;
    }

}
