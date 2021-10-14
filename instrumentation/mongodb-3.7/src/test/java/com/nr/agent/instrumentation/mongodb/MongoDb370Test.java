/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
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
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.mongodb")
public class MongoDb370Test {

    private static final String MONGODB_PRODUCT = DatastoreVendor.MongoDB.toString();
    private static final MongodStarter mongodStarter = MongodStarter.getDefaultInstance();
    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;
    private MongoClient mongoClient;

    @Before
    public void startMongo() throws Exception {
        int port = Network.getFreeServerPort();
        MongodConfig mongodConfig = ImmutableMongodConfig.builder()
                .version(Version.V3_6_5)
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
    public void testCRUD() throws Exception {
        demoCRUD(new PokemonMaster(mongoClient));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(1000));

        DatastoreHelper helper = new DatastoreHelper(MONGODB_PRODUCT);
        helper.assertAggregateMetrics();

        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();
        helper.assertUnifiedMetricCounts(txName, "insert", "pokemon", 1);
        helper.assertUnifiedMetricCounts(txName, "find", "pokemon", 2);
        helper.assertUnifiedMetricCounts(txName, "update", "pokemon", 1);
        helper.assertUnifiedMetricCounts(txName, "delete", "pokemon", 1);

        assertEquals(5, MetricsHelper.getUnscopedMetricCount("Datastore/all"));
        assertEquals(5, (int)introspector.getTransactionEvents(txName).iterator().next().getDatabaseCallCount());
    }

    @Trace(dispatcher = true)
    public static void demoCRUD(PokemonMaster master) throws InterruptedException {
        System.out.println("===Basic CRUD===");
        Document newPokemon = new Document("name", "Togepi").append("number", 175).append("type", "fire");
        master.demoInsert(newPokemon);

        System.out.print("(R) Find one fire type: " + master.demoFindOne("fire"));
        System.out.print(". Find: Original 150: ");
        master.demoFind();
        for (Document pokemon : master.demoFind()) {
            System.out.print(pokemon.get("name") + " ");
        }
        System.out.println();
        System.out.println("(U) Update results: " + master.demoUpdate(newPokemon, (new Document("$set", new Document("type", "fairy")))));
        System.out.println("(D) Delete results: " + master.demoRemove(newPokemon));
    }

    @Test
    public void testCollectionApi() throws Exception {
        runQuickTour();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(1000));

        DatastoreHelper helper = new DatastoreHelper(MONGODB_PRODUCT);
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("dropDatabase", 1);

        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();
        helper.assertUnifiedMetricCounts(txName, "insert", "test", 4);
        helper.assertUnifiedMetricCounts(txName, "update", "test", 6);
        helper.assertUnifiedMetricCounts(txName, "delete", "test", 4);
        helper.assertUnifiedMetricCounts(txName, "find", "test", 13);
        helper.assertUnifiedMetricCounts(txName, "drop", "test", 3);
        helper.assertUnifiedMetricCounts(txName, "count", "test", 1);
        helper.assertUnifiedMetricCounts(txName, "getMore", "test", 12);

        assertEquals(44, MetricsHelper.getUnscopedMetricCount("Datastore/all"));
        assertEquals(44, (int)introspector.getTransactionEvents(txName).iterator().next().getDatabaseCallCount());
    }

    @Trace(dispatcher = true)
    public void runQuickTour() throws Exception {
        QuickTour.run(mongoClient);
    }

}
