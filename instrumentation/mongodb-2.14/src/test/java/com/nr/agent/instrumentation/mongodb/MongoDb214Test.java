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
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.extract.DirectoryAndExecutableNaming;
import de.flapdoodle.embed.process.extract.TempNaming;
import de.flapdoodle.embed.process.io.directories.PropertyOrPlatformTempDir;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.ExtractedArtifactStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.mongodb")
public class MongoDb214Test {

    private static final String MONGODB_PRODUCT = DatastoreVendor.MongoDB.toString();
    private static final MongodStarter mongodStarter;

    static {
        Command command = Command.MongoD;
        RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(command)
                .artifactStore(ExtractedArtifactStore.builder()
                        .from(Defaults.extractedArtifactStoreFor(command))
                        .temp(DirectoryAndExecutableNaming.builder()
                                .directory(new PropertyOrPlatformTempDir())
                                // The default configuration creates executables whose names contain random UUIDs, which
                                // prompts repetitive firewall dialog popups. Instead, we use a naming strategy that
                                // produces a stable executable name and only have to acknowledge the firewall dialogs once.
                                // This firewall dialog issue only seems to occur with versions of mongo < 3.6.0
                                .executableNaming(new TempNaming() {
                                    @Override
                                    public String nameFor(String prefix, String postfix) {
                                        return prefix + "-Db214-" + postfix;
                                    }
                                })
                                .build())
                        .build())
                .build();
        mongodStarter = MongodStarter.getInstance(runtimeConfig);
    }

    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;
    private MongoClient mongoClient;

    @Before
    public void startMongo() throws Exception {
        int port = Network.getFreeServerPort();
        @SuppressWarnings("deprecation")
        ImmutableMongodConfig mongodConfig = ImmutableMongodConfig.builder()
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
