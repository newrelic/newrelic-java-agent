package com.nr.agent.instrumentation.mongodb;

/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.nr.agent.instrumentation.mongodb.SubscriberHelpers.ObservableSubscriber;
import com.nr.agent.instrumentation.mongodb.SubscriberHelpers.OperationSubscriber;
import com.nr.agent.instrumentation.mongodb.SubscriberHelpers.PrintDocumentSubscriber;
import com.nr.agent.instrumentation.mongodb.SubscriberHelpers.PrintSubscriber;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
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
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * The QuickTour code example
 * https://github.com/mongodb/mongo-java-driver/blob/5911dfb1811fb3e6946a6bddf65cdaf0ac774b63/driver-reactive-streams/src/examples/reactivestreams/tour/QuickTour.java
 *
 * Adapted from https://mongodb.github.io/mongo-java-driver/4.0/driver-reactive/getting-started/quick-start/
 */
public class QuickTour {
    /**
     * Exercises MongoDB reactive APIs.
     *
     * @param mongoClient MongoClient instance
     */
    public static void run(MongoClient mongoClient) {
        // get handle to "mydb" database
        MongoDatabase database = mongoClient.getDatabase("mydb");

        // get a handle to the "test" collection
        final MongoCollection<Document> collection = database.getCollection("test");

        // drop all the data in it
        ObservableSubscriber<Void> successSubscriber = new OperationSubscriber<>();
        collection.drop().subscribe(successSubscriber);
        successSubscriber.await();

        // make a document and insert it
        Document doc = new Document("name", "MongoDB")
                .append("type", "database")
                .append("count", 1)
                .append("info", new Document("x", 203).append("y", 102));

        ObservableSubscriber<InsertOneResult> insertOneSubscriber = new OperationSubscriber<>();
        collection.insertOne(doc).subscribe(insertOneSubscriber);
        insertOneSubscriber.await();

        // get it (since it's the only one in there since we dropped the rest earlier on)
        ObservableSubscriber<Document> documentSubscriber = new PrintDocumentSubscriber();
        collection.find().first().subscribe(documentSubscriber);
        documentSubscriber.await();

        // now, lets add lots of little documents to the collection so we can explore queries and cursors
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }

        ObservableSubscriber<InsertManyResult> insertManySubscriber = new OperationSubscriber<>();
        collection.insertMany(documents).subscribe(insertManySubscriber);
        insertManySubscriber.await();

        // count documents
        collection.countDocuments().subscribe(new PrintSubscriber<>("total # of documents after inserting 100 small ones (should be 101): %s"));

        // find first
        documentSubscriber = new PrintDocumentSubscriber();
        collection.find().first().subscribe(documentSubscriber);
        documentSubscriber.await();

        // lets get all the documents in the collection and print them out
        documentSubscriber = new PrintDocumentSubscriber();
        collection.find().subscribe(documentSubscriber);
        documentSubscriber.await();

        // Query Filters

        // now use a query to get 1 document out
        documentSubscriber = new PrintDocumentSubscriber();
        collection.find(eq("i", 71)).first().subscribe(documentSubscriber);
        documentSubscriber.await();

        // now use a range query to get a larger subset
        documentSubscriber = new PrintDocumentSubscriber();
        collection.find(gt("i", 50)).subscribe(documentSubscriber);
        successSubscriber.await();

        // range query with multiple constraints
        documentSubscriber = new PrintDocumentSubscriber();
        collection.find(and(gt("i", 50), lte("i", 100))).subscribe(documentSubscriber);
        successSubscriber.await();

        // Sorting
        documentSubscriber = new PrintDocumentSubscriber();
        collection.find(exists("i")).sort(descending("i")).first().subscribe(documentSubscriber);
        documentSubscriber.await();

        // Projection
        documentSubscriber = new PrintDocumentSubscriber();
        collection.find().projection(excludeId()).first().subscribe(documentSubscriber);
        documentSubscriber.await();

        // Aggregation
        documentSubscriber = new PrintDocumentSubscriber();
        collection.aggregate(asList(
                match(gt("i", 0)),
                project(Document.parse("{ITimes10: {$multiply: ['$i', 10]}}")))
        ).subscribe(documentSubscriber);
        documentSubscriber.await();

        documentSubscriber = new PrintDocumentSubscriber();
        collection.aggregate(singletonList(group(null, sum("total", "$i"))))
                .first().subscribe(documentSubscriber);
        documentSubscriber.await();

        // Update One
        ObservableSubscriber<UpdateResult> updateSubscriber = new OperationSubscriber<>();
        collection.updateOne(eq("i", 10), set("i", 110)).subscribe(updateSubscriber);
        updateSubscriber.await();

        // Update Many
        updateSubscriber = new OperationSubscriber<>();
        collection.updateMany(lt("i", 100), inc("i", 100)).subscribe(updateSubscriber);
        updateSubscriber.await();




        // 1. Ordered bulk operation - order is guaranteed
        collection.bulkWrite(
                        Arrays.asList(new InsertOneModel<>(new Document("_id", 4)),
                                new InsertOneModel<>(new Document("_id", 5)),
                                new InsertOneModel<>(new Document("_id", 6)),
                                new UpdateOneModel<>(new Document("_id", 1),
                                        new Document("$set", new Document("x", 2))),
                                new DeleteOneModel<>(new Document("_id", 2)),
                                new ReplaceOneModel<>(new Document("_id", 3),
                                        new Document("_id", 3).append("x", 4))))
                .subscribe(new OperationSubscriber<>());


        // 2. Unordered bulk operation - no guarantee of order of operation
        collection.bulkWrite(
                        Arrays.asList(new InsertOneModel<>(new Document("_id", 4)),
                                new InsertOneModel<>(new Document("_id", 5)),
                                new InsertOneModel<>(new Document("_id", 6)),
                                new UpdateOneModel<>(new Document("_id", 1),
                                        new Document("$set", new Document("x", 2))),
                                new DeleteOneModel<>(new Document("_id", 2)),
                                new ReplaceOneModel<>(new Document("_id", 3),
                                        new Document("_id", 3).append("x", 4))),
                        new BulkWriteOptions().ordered(false))
                .subscribe(new OperationSubscriber<>());




        // Delete One
        ObservableSubscriber<DeleteResult> deleteSubscriber = new OperationSubscriber<>();
        collection.deleteOne(eq("i", 110)).subscribe(deleteSubscriber);
        deleteSubscriber.await();

        // Delete Many
        deleteSubscriber = new OperationSubscriber<>();
        collection.deleteMany(gte("i", 100)).subscribe(deleteSubscriber);
        deleteSubscriber.await();

        // Create Index
        OperationSubscriber<String> createIndexSubscriber = new PrintSubscriber<>("Create Index Result: %s");
        collection.createIndex(new Document("i", 1)).subscribe(createIndexSubscriber);
        createIndexSubscriber.await();

        // Clean up
        successSubscriber = new OperationSubscriber<>();
        collection.drop().subscribe(successSubscriber);
        successSubscriber.await();

        // release resources
        mongoClient.close();
    }
}
