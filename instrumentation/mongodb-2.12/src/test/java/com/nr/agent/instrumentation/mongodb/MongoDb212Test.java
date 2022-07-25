/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.mongodb")
public class MongoDb212Test {

    private static final String MONGODB_PRODUCT = DatastoreVendor.MongoDB.toString();
    private static final MongodStarter mongodStarter;

    static {
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder().defaults(Command.MongoD)
                .artifactStore(new ExtractedArtifactStoreBuilder()
                    .defaults(Command.MongoD)
                    // The default configuration creates executables whose names contain random UUIDs, which
                    // prompts repetitive firewall dialog popups. Instead, we use a naming strategy that
                    // produces a stable executable name and only have to acknowledge the firewall dialogs once.
                    // On macOS systems, the dialogs must be acknowledged quickly in order to be registered.
                    // Failure to click fast enough will result in additional dialogs on subsequent test runs.
                    // This firewall dialog issue only seems to occur with versions of mongo < 3.6.0
                    .executableNaming(new ITempNaming() {
                        @Override
                        public String nameFor(String prefix, String postfix) {
                            return prefix + "-Db212-" + postfix;
                        }
                    }))
                .build();
        mongodStarter = MongodStarter.getInstance(runtimeConfig);
    }

    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;
    private MongoClient mongoClient;

    @Before
    public void startMongo() throws Exception {
        int port = Network.getFreeServerPort();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.V2_6_11)
                .net(new Net(port, Network.localhostIsIPv6()))
                .build();

        mongodExecutable = mongodStarter.prepare(mongodConfig);
        mongodProcess = mongodExecutable.start();
        mongoClient = new MongoClient("localhost", port);
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
        assertEquals(1, introspector.getFinishedTransactionCount());

        DatastoreHelper helper = new DatastoreHelper(MONGODB_PRODUCT);
        helper.assertAggregateMetrics();

        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();
        helper.assertUnifiedMetricCounts(txName, "insert", "pokemon", 1);
        helper.assertUnifiedMetricCounts(txName, "find", "pokemon", 2);
        helper.assertUnifiedMetricCounts(txName, "update", "pokemon", 1);
        helper.assertUnifiedMetricCounts(txName, "remove", "pokemon", 1);
    }

    @Trace(dispatcher = true)
    public static void demoCRUD(PokemonMaster master) throws InterruptedException {
        System.out.println("===Basic CRUD===");
        DBObject newPokemon = new BasicDBObject("name", "Togepi").append("number", 175);
        System.out.println("(C) Insert results: " + master.demoInsert(newPokemon));

        System.out.print("(R) Find one fire type: " + master.demoFindOne("fire"));
        System.out.print(". Find: Original 150: ");
        for (DBObject pokemon : master.demoFind()) {
            System.out.print(pokemon.get("name") + " ");
        }
        System.out.println();
        System.out.println("(U) Update results: " + master.demoUpdate(newPokemon, new BasicDBObject("type", "fairy")));
        System.out.println("(D) Delete results: " + master.demoRemove(newPokemon));
    }

    @Test
    public void testBulkOps() throws Exception {
        demoBulkOps(new PokemonMaster(mongoClient));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        DatastoreHelper helper = new DatastoreHelper(MONGODB_PRODUCT);
        helper.assertAggregateMetrics();

        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();
        helper.assertUnifiedMetricCounts(txName, "insert", "pokemon", 3);
        helper.assertUnifiedMetricCounts(txName, "parallelCollectionScan", "pokemon", 1);
    }

    @Trace(dispatcher = true)
    public static void demoBulkOps(PokemonMaster master) throws InterruptedException {
        master.demoInsert(new BasicDBObject("name", "Togepi").append("number", 175));

        System.out.println("===Bulk Operations===");
        System.out.print("Parallel Scan: ");
        ArrayList<DBObject> parallelScanResults = master.demoParallelScan();
        for (DBObject pokemon : parallelScanResults) {
            System.out.print(pokemon.get("number") + "-" + pokemon.get("name") + ", ");
        }
        System.out.println();

        System.out.println("Ordered Bulk Operations: " + master.demoBulkOperationOrdered());
        System.out.println("Unordered Bulk Operations: " + master.demoBulkOperationUnordered());
    }

    @Test
    public void testAggregation() throws Exception {
        demoAggregation(new PokemonMaster(mongoClient));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        DatastoreHelper helper = new DatastoreHelper(MONGODB_PRODUCT);
        helper.assertAggregateMetrics();

        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();
        helper.assertUnifiedMetricCounts(txName, "aggregate", "pokemon", 2);
    }

    @Trace(dispatcher = true)
    public static void demoAggregation(PokemonMaster master) throws InterruptedException {
        System.out.println("===Aggregate Operations===");
        System.out.print("Aggregation results (Average Attack by type desc): ");
        for (DBObject pokemon : master.demoAggregationResults()) {
            System.out.print(pokemon.get("_id") + "::" + pokemon.get("AverageAttack") + ", ");
        }
        System.out.println();

        System.out.print("Aggregation Cursor  (Average Attack by type desc): ");
        for (DBObject pokemon : master.demoAggregationCursor()) {
            System.out.print(pokemon.get("_id") + "::" + pokemon.get("AverageAttack") + ", ");
        }
        System.out.println();
    }

    @Test
    public void testFindAndModify() throws Exception {
        demoFindAndModify(new PokemonMaster(mongoClient));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        DatastoreHelper helper = new DatastoreHelper(MONGODB_PRODUCT);
        helper.assertAggregateMetrics();

        Collection<String> transactionNames = InstrumentationTestRunner.getIntrospector().getTransactionNames();
        assertEquals(1, transactionNames.size());
        String txName = transactionNames.iterator().next();
        helper.assertUnifiedMetricCounts(txName, "findandmodify", "pokemon", 1);
    }

    @Trace(dispatcher = true)
    public static void demoFindAndModify(PokemonMaster master) throws InterruptedException {
        System.out.println("===FindAndModify===");
        System.out.println("(R) Find and modify one fire type: " + master.demoFindAndModify("fire"));
        System.out.println();
    }
}
