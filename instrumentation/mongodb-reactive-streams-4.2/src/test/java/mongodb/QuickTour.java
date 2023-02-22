package mongodb;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
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
    public static void main(final String[] args) throws Throwable {
        //doTxn(args);
        MongoClient client = getClient(args);
        MongoCollection<Document> collection = getCollection(client);
        dropEverything(collection);
        insertOne(collection);
        getFirst(collection);
        insertMany(collection);
        count(collection);
        updateOne(collection);
        variousFinds(collection);
        deleteOne(collection);
        deleteMany(collection);
        dropEverything(collection);
    }

    @Trace
    public static MongoClient getClient(String [] args) {
        MongoClient mongoClient;

        if (args.length == 0) {
            // connect to the local database server
            mongoClient = MongoClients.create();
            //mongoClient = MongoClients.create("mongodb+srv://bob:pw@localhost/mydb");
//            mongoClient = MongoClients.create("mongodb://mongodb0.example.com:27017");
        } else {
            mongoClient = MongoClients.create(args[0]);
        }

        System.out.println("------ " + mongoClient.getClusterDescription().getShortDescription());
        System.out.println("------ " + mongoClient.getClusterDescription().getServerDescriptions().get(0).getAddress());

        return mongoClient;
    }

    @Trace
    public static MongoCollection<Document> getCollection(MongoClient client) {
        MongoDatabase database = client.getDatabase("mydb");

        // get a handle to the "test" collection
        return database.getCollection("random-junk");
    }

    @Trace
    public static void dropEverything(MongoCollection<Document> collection) throws Throwable {
        SubscriberHelpers.ObservableSubscriber subscriber = new SubscriberHelpers.ObservableSubscriber<Success>();
        collection.drop().subscribe(subscriber);
        subscriber.await();
    }

    @Trace
    public static void insertOne(MongoCollection<Document> collection) {
        Document doc = new Document("name", "MongoDB")
                .append("type", "database")
                .append("count", 1)
                .append("info", new Document("x", 203).append("y", 102));

        collection.insertOne(doc).subscribe(new SubscriberHelpers.OperationSubscriber<>());
    }

    @Trace
    public static void getFirst(MongoCollection<Document> collection) {
        collection.find().first().subscribe(new SubscriberHelpers.PrintDocumentSubscriber());
    }

    @Trace
    public static void insertMany(MongoCollection<Document> collection) throws Throwable {
        List<Document> documents = new ArrayList<Document>();
        for (int i = 0; i < 500; i++) {
            documents.add(new Document("i", i));
        }

        SubscriberHelpers.ObservableSubscriber subscriber = new SubscriberHelpers.ObservableSubscriber<Success>();
        collection.insertMany(documents).subscribe(subscriber);
        subscriber.await();
    }

    @Trace
    public static void count(MongoCollection<Document> collection) throws Throwable {
        collection.countDocuments()
                .subscribe(new SubscriberHelpers.PrintSubscriber<Long>("total # of documents after inserting 500 small ones (should be 501): %s"));
    }

    @Trace
    public static void updateMany(MongoCollection<Document> collection) throws Throwable {
        SubscriberHelpers.ObservableSubscriber subscriber = subscriber = new SubscriberHelpers.PrintSubscriber<UpdateResult>("Update Result: %s");
        collection.updateMany(lt("i", 100), new Document("$inc", new Document("i", 100))).subscribe(subscriber);
        subscriber.await();
    }

    @Trace
    public static void updateOne(MongoCollection<Document> collection) {
        collection.updateOne(eq("i", 10), new Document("$set", new Document("i", 110)))
                .subscribe(new SubscriberHelpers.PrintSubscriber<UpdateResult>("Update Result: %s"));
    }

    @Trace
    public static void deleteOne(MongoCollection<Document> collection) {
        collection.deleteOne(eq("i", 110)).subscribe(new SubscriberHelpers.PrintSubscriber<DeleteResult>("Delete Result: %s"));
    }

    @Trace
    public static void deleteMany(MongoCollection<Document> collection) {
        collection.deleteMany(gte("i", 100)).subscribe(new SubscriberHelpers.PrintSubscriber<DeleteResult>("Delete Result: %s"));
    }

    @Trace
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

    @Trace(dispatcher = true)
    public static void doTxn(final String[] args) throws Throwable {
        MongoClient mongoClient;

        if (args.length == 0) {
            // connect to the local database server
            mongoClient = MongoClients.create();
            //mongoClient = MongoClients.create("mongodb+srv://bob:pw@localhost/mydb");
//            mongoClient = MongoClients.create("mongodb://mongodb0.example.com:27017");
        } else {
            mongoClient = MongoClients.create(args[0]);
        }

        System.out.println("------ " + mongoClient.getClusterDescription().getShortDescription());
        System.out.println("------ " + mongoClient.getClusterDescription().getServerDescriptions().get(0).getAddress());

        // get handle to "mydb" database
        MongoDatabase database = mongoClient.getDatabase("mydb");

        // get a handle to the "test" collection
        MongoCollection<Document> collection = database.getCollection("random-junk");

        // drop all the data in it
        SubscriberHelpers.ObservableSubscriber subscriber = new SubscriberHelpers.ObservableSubscriber<Success>();
        collection.drop().subscribe(subscriber);
        subscriber.await();

        // make a document and insert it
        Document doc = new Document("name", "MongoDB")
                .append("type", "database")
                .append("count", 1)
                .append("info", new Document("x", 203).append("y", 102));

        collection.insertOne(doc).subscribe(new SubscriberHelpers.OperationSubscriber<>());

        // get it (since it's the only one in there since we dropped the rest earlier on)
        collection.find().first().subscribe(new SubscriberHelpers.PrintDocumentSubscriber());

        // now, lets add lots of little documents to the collection so we can explore queries and cursors
        List<Document> documents = new ArrayList<Document>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }

        subscriber = new SubscriberHelpers.ObservableSubscriber<Success>();
        collection.insertMany(documents).subscribe(subscriber);
        subscriber.await();

        collection.countDocuments()
                .subscribe(new SubscriberHelpers.PrintSubscriber<Long>("total # of documents after inserting 100 small ones (should be 101): %s"));

        subscriber = new SubscriberHelpers.PrintDocumentSubscriber();
        collection.find().first().subscribe(subscriber);
        subscriber.await();

        subscriber = new SubscriberHelpers.PrintDocumentSubscriber();
        collection.find().subscribe(subscriber);
        subscriber.await();

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

        // Update One
        collection.updateOne(eq("i", 10), new Document("$set", new Document("i", 110)))
                .subscribe(new SubscriberHelpers.PrintSubscriber<UpdateResult>("Update Result: %s"));


        // Update Many
        subscriber = new SubscriberHelpers.PrintSubscriber<UpdateResult>("Update Result: %s");
        collection.updateMany(lt("i", 100), new Document("$inc", new Document("i", 100))).subscribe(subscriber);
        subscriber.await();

        // Delete One
        collection.deleteOne(eq("i", 110)).subscribe(new SubscriberHelpers.PrintSubscriber<DeleteResult>("Delete Result: %s"));

        // Delete Many
        collection.deleteMany(gte("i", 100)).subscribe(new SubscriberHelpers.PrintSubscriber<DeleteResult>("Delete Result: %s"));

        subscriber = new SubscriberHelpers.ObservableSubscriber<Success>();
        collection.drop().subscribe(subscriber);
        subscriber.await();

        // ordered bulk writes
        List<WriteModel<Document>> writes = new ArrayList<WriteModel<Document>>();
        writes.add(new InsertOneModel<Document>(new Document("_id", 4)));
        writes.add(new InsertOneModel<Document>(new Document("_id", 5)));
        writes.add(new InsertOneModel<Document>(new Document("_id", 6)));
        writes.add(new UpdateOneModel<Document>(new Document("_id", 1), new Document("$set", new Document("x", 2))));
        writes.add(new DeleteOneModel<Document>(new Document("_id", 2)));
        writes.add(new ReplaceOneModel<Document>(new Document("_id", 3), new Document("_id", 3).append("x", 4)));

        subscriber = new SubscriberHelpers.PrintSubscriber("Bulk write results: %s");
        collection.bulkWrite(writes).subscribe(subscriber);
        subscriber.await();

        subscriber = new SubscriberHelpers.ObservableSubscriber();
        collection.drop().subscribe(subscriber);
        subscriber.await();

        subscriber = new SubscriberHelpers.PrintSubscriber<BulkWriteResult>("Bulk write results: %s");
        collection.bulkWrite(writes, new BulkWriteOptions().ordered(false)).subscribe(subscriber);
        subscriber.await();

        subscriber = new SubscriberHelpers.PrintDocumentSubscriber();
        collection.find().subscribe(subscriber);
        subscriber.await();

        // Clean up
        subscriber = new SubscriberHelpers.PrintSubscriber("Collection Dropped");
        collection.drop().subscribe(subscriber);
        subscriber.await();

        // release resources
        mongoClient.close();
    }

    private QuickTour() {
    }
}

