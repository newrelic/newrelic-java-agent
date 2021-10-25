/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.mongodb;

import com.mongodb.ServerAddress;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.logging.Level;

/**
 * A CommandListener which reports mongo events to New Relic APM.
 */
public class NewRelicCommandListener implements CommandListener {

    private static final String OP_GET_MORE = "getMore";
    private static final String COLLECTION_KEY = "collection";

    private final ThreadLocal<Segment> holder = new ThreadLocal<>();

    @Override
    public void commandStarted(CommandStartedEvent event) {
        try {
            Segment tracer = NewRelic.getAgent().getTransaction().startSegment(null);

            if (tracer != null) {
                holder.set(tracer);

                String collectionName = getCollectionName(event);
                String operationName = event.getCommandName().intern();
                ServerAddress address = event.getConnectionDescription().getServerAddress();

                DatastoreParameters params = DatastoreParameters
                        .product(DatastoreVendor.MongoDB.name())
                        .collection(collectionName)
                        .operation(operationName)
                        .instance(address.getHost(), address.getPort())
                        .databaseName(event.getDatabaseName())
                        .build();
                tracer.reportAsExternal(params);
            }
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        tryFinishEvent();
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        tryFinishEvent();
    }

    private void tryFinishEvent() {
        try {
            Segment tracerEvent = holder.get();
            if (tracerEvent != null) {
                tracerEvent.end();
                holder.remove();
            }
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
    }

    private String getCollectionName(CommandStartedEvent event) {
        // Fallback to database name if we can't get collection name
        String collectionName = event.getDatabaseName();

        try {
            BsonDocument command = event.getCommand();
            String commandName = event.getCommandName();

            // We can grab the collection name with the command name, except for GetMore operations.
            BsonValue bsonValue = OP_GET_MORE.equals(commandName) ? command.get(COLLECTION_KEY)
                    : command.get(commandName);
            if (bsonValue instanceof BsonString) {
                collectionName = ((BsonString) bsonValue).getValue();
            }
        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINER,
                    "WARNING: Could not determine mongo collection name for command {0}. Using database name. ",
                    event.getCommandName());
        }

        return collectionName;
    }

}
