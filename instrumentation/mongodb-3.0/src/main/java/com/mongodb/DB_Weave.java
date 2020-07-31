/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb;

import java.util.NoSuchElementException;
import java.util.logging.Level;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

@Weave(originalName = "com.mongodb.DB")
public abstract class DB_Weave {

    /*
     * Command calls db.$cmd.find({command}). Since that isn't actually a find operation we want to include its metrics
     * in this tracer. Hence, this tracer is a leaf.
     */
    @Trace(leaf = true)
    public CommandResult command(final DBObject command, final ReadPreference readPreference, final DBEncoder encoder) {
        String commandName = null;
        String collectionName = null;
        try {
            commandName = command.keySet().iterator().next();
            collectionName = String.valueOf(command.get(commandName));
        } catch (NoSuchElementException nse) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "WARNING: Could not determine mongo command name.");
            commandName = MongoUtil.DEFAULT_OPERATION;
            collectionName = MongoUtil.DEFAULT_COLLECTION;
        }

        CommandResult result = Weaver.callOriginal();

        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();

        DatastoreParameters params = DatastoreParameters
                .product(DatastoreVendor.MongoDB.name())
                .collection(collectionName)
                .operation(commandName)
                .instance(getMongo().getAddress().getHost(), getMongo().getAddress().getPort())
                .databaseName(getName())
                .build();
        tracedMethod.reportAsExternal(params);

        return result;
    }

    public abstract Mongo getMongo();

    public abstract String getName();

}
