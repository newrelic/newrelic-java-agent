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
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * Instrumentation for all Database Commands
 */
@Weave(type = MatchType.BaseClass, originalName = "com.mongodb.DB")
public abstract class DB_Weave {
    @NewField
    private static final String DEFAULT_OP = "other";
    @NewField
    private static final String DEFAULT_COLLECTION = "other";

    private void instrument(TracedMethod method, String operation, String collection, String serverUsed,
                            int serverPortUsed) {

        DatastoreParameters params = DatastoreParameters
                .product(DatastoreVendor.MongoDB.name())
                .collection(collection)
                .operation(operation)
                .instance(serverUsed, serverPortUsed)
                .databaseName(getName())
                .build();
        method.reportAsExternal(params);
    }

    /*
     * Command calls db.$cmd.find({command}). Since that isn't actually a find operation we want to include its metrics
     * in this tracer. Hence, this tracer is a leaf.
     */
    @Trace(leaf = true)
    public CommandResult command(DBObject cmd, int options, ReadPreference readPreference, DBEncoder encoder) {
        String command = null;
        String collection = null;
        try {
            command = cmd.keySet().iterator().next();
            collection = String.valueOf(cmd.get(command));
        } catch (NoSuchElementException nse) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "WARNING: Could not determine mongo command name.");
            command = DEFAULT_OP;
            collection = DEFAULT_COLLECTION;
        }

        CommandResult result = Weaver.callOriginal();
        instrument(NewRelic.getAgent().getTracedMethod(), command, collection, result.getServerUsed().getHost(),
                result.getServerUsed().getPort());
        return result;
    }

    public abstract Mongo getMongo();

    public abstract String getName();

}
