package mongodb;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.newrelic.api.agent.Trace;
import org.bson.Document;

import java.util.ArrayList;
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

/**
 * The QuickTour code example
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class QuickTour {

    public enum Success {

        /**
         * A Successful operation
         */
        SUCCESS
    }

    @Trace(dispatcher = true)
    public static void run(MongoClient mongoClient, MongoCollection collection) throws Throwable {
        //doTxn(args);
        dropEverything(collection);
        insertOne(collection);
        getFirst(collection); // This will count as a find transaction
        variousFinds(collection); // five calls to find here for total of 6 with getFirst()
        insertMany(collection);
        updateMany(collection);
        count(collection); // Disabled until we figure out why CountOperation_Instrumentation doesn't record Datastore metrics
        updateOne(collection);
        deleteOne(collection);
        deleteMany(collection);
        dropEverything(collection);
    }

    public static void dropEverything(MongoCollection<Document> collection) throws Throwable {
        SubscriberHelpers.ObservableSubscriber subscriber = new SubscriberHelpers.ObservableSubscriber<Success>();
        collection.drop().subscribe(subscriber);
        subscriber.await();
    }

    public static void insertOne(MongoCollection<Document> collection) {
        Document doc = new Document("name", "MongoDB")
                .append("type", "database")
                .append("count", 1)
                .append("info", new Document("x", 203).append("y", 102));

        collection.insertOne(doc).subscribe(new SubscriberHelpers.OperationSubscriber<>());
    }

    public static void getFirst(MongoCollection<Document> collection) {
        collection.find().first().subscribe(new SubscriberHelpers.PrintDocumentSubscriber());
    }

    public static void insertMany(MongoCollection<Document> collection) throws Throwable {
        List<Document> documents = new ArrayList<Document>();
        for (int i = 0; i < 500; i++) {
            documents.add(new Document("i", i));
        }

        SubscriberHelpers.ObservableSubscriber subscriber = new SubscriberHelpers.ObservableSubscriber<Success>();
        collection.insertMany(documents).subscribe(subscriber);
        subscriber.await();
    }

    public static void count(MongoCollection<Document> collection) throws Throwable {
        collection.countDocuments()
                .subscribe(new SubscriberHelpers.PrintSubscriber<Long>("total # of documents after inserting 500 small ones (should be 501): %s"));
    }

    public static void updateMany(MongoCollection<Document> collection) throws Throwable {
        SubscriberHelpers.ObservableSubscriber subscriber = subscriber = new SubscriberHelpers.PrintSubscriber<UpdateResult>("Update Result: %s");
        collection.updateMany(lt("i", 100), new Document("$inc", new Document("i", 100))).subscribe(subscriber);
        subscriber.await();
    }

    public static void updateOne(MongoCollection<Document> collection) {
        collection.updateOne(eq("i", 10), new Document("$set", new Document("i", 110)))
                .subscribe(new SubscriberHelpers.PrintSubscriber<UpdateResult>("Update Result: %s"));
    }

    public static void deleteOne(MongoCollection<Document> collection) {
        collection.deleteOne(eq("i", 110)).subscribe(new SubscriberHelpers.PrintSubscriber<DeleteResult>("Delete Result: %s"));
    }

    public static void deleteMany(MongoCollection<Document> collection) {
        collection.deleteMany(gte("i", 100)).subscribe(new SubscriberHelpers.PrintSubscriber<DeleteResult>("Delete Result: %s"));
    }

    public static void variousFinds(MongoCollection<Document> collection) {
        // Query Filters
        // now use a query to get 1 document out
        collection.find(eq("i", 71)).first().subscribe(new SubscriberHelpers.PrintDocumentSubscriber());

        // now use a range query to get a larger subset
        collection.find(gt("i", 50)).subscribe(new SubscriberHelpers.PrintDocumentSubscriber());

        // range query with multiple constraints
        collection.find(and(gt("i", 50), lte("i", 100))).subscribe(new SubscriberHelpers.PrintDocumentSubscriber());

        // Sorting
        collection.find(exists("i")).sort(descending("i")).first().subscribe(new SubscriberHelpers.PrintDocumentSubscriber());

        // Projection
        collection.find().projection(excludeId()).first().subscribe(new SubscriberHelpers.PrintDocumentSubscriber());
    }
    private QuickTour() {
    }
}

