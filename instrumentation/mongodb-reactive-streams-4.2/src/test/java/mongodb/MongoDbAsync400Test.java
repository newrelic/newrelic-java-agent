/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.api.agent.Trace;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static de.flapdoodle.embed.process.runtime.Network.getLocalHost;
import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.mongodb")
public class MongoDbAsync400Test {

    private static final String MONGODB_PRODUCT = DatastoreVendor.MongoDB.toString();
    private static final MongodStarter mongodStarter = MongodStarter.getDefaultInstance();
    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;
    private MongoClient mongoClient;

    @Before
    public void startMongo() throws Exception {
        int port = Network.freeServerPort(getLocalHost());
        MongodConfig mongodConfig = ImmutableMongodConfig.builder()
                .version(Version.V4_0_2) // MongoDB version, not Mongo client version 4.0.26
                .net(new Net(port, Network.localhostIsIPv6()))
                .build();
        mongodExecutable = mongodStarter.prepare(mongodConfig);
        mongodProcess = mongodExecutable.start();
        mongoClient = MongoClients.create(new ConnectionString("mongodb://localhost:" + port));
    }

    @After
    public void stopMongo() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (mongodProcess != null) {
            mongodProcess.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
    }

    @Test
    public void testCollectionApi() throws Exception {
        runMongoQuickTour();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(1000));

        DatastoreHelper helper = new DatastoreHelper(MONGODB_PRODUCT);
        helper.assertAggregateMetrics();

        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();

        // Counts based on operations executed in QuickTour
        int insertOpExpectedCount = 1;
        int insertManyOpExpectedCount = 1;
        int updateOpExpectedCount = 1;
        int updateManyOpExpectedCount = 1;
        int deleteOpExpectedCount = 1;
        int deleteManyOpExpectedCount = 1;
        int findOpExpectedCount = 8;
        int dropOpExpectedCount = 2;
        int countOpExpectedCount = 1;
        int bulkWriteOpExpectedCount = 2;
        int createIndexesOpExpectedCount = 1;
        int unknownOpExpectedCount = 3;

        helper.assertUnifiedMetricCounts(txName, "insert", "test", insertOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "insertMany", "test", insertManyOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "update", "test", updateOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "updateMany", "test", updateManyOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "delete", "test", deleteOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "deleteMany", "test", deleteManyOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "find", "test", findOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "drop", "test", dropOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "count", "test", countOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "bulkWrite", "test", bulkWriteOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "createIndexes", "test", createIndexesOpExpectedCount);
        helper.assertUnifiedMetricCounts(txName, "Unknown", "Unknown", unknownOpExpectedCount);

        int totalOpCount = insertOpExpectedCount + insertManyOpExpectedCount + updateOpExpectedCount + updateManyOpExpectedCount +
                deleteOpExpectedCount + deleteManyOpExpectedCount + findOpExpectedCount + dropOpExpectedCount + countOpExpectedCount +
                bulkWriteOpExpectedCount + createIndexesOpExpectedCount + unknownOpExpectedCount;

        // Should be equal to the sum of all above metric counts
        assertEquals(totalOpCount, MetricsHelper.getUnscopedMetricCount("Datastore/all"));
        assertEquals(totalOpCount, MetricsHelper.getUnscopedMetricCount("Datastore/MongoDB/allOther"));
        assertEquals(totalOpCount, introspector.getTransactionEvents(txName).iterator().next().getDatabaseCallCount());
    }

    @Trace(dispatcher = true)
    public void runMongoQuickTour() throws Exception {
        //QuickTour.run(mongoClient);
    }

}
