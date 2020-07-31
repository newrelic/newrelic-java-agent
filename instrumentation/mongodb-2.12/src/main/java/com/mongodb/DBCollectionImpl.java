/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb;

import java.util.List;

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
 * This is the most common point of instrumentation. Anything lower and we lose needed information.
 * DBTCPConnector and DBPort look tempting, but they have been found to not work.
 * 
 * There are several code paths to consider:
 *   -Basic CRUD Operations
 *   -Bulk Operations
 *   -Database Commands (Instrumented in the DB class)
 *   -Index creation
 * All of these code paths interact with Mongo in slightly different ways.  
 * 
 * 
 * Bulk operations are the reason the implementing class was chosen instead of the abstract class.
 * The writeWithCommandProtocol is the common bulk method and it is not defined in the abstract class.
 */
@Weave(type = MatchType.ExactClass)
abstract class DBCollectionImpl extends DBCollection {

    @NewField
    private static final String OP_FIND = "find";
    @NewField
    private static final String OP_INSERT = "insert";
    @NewField
    private static final String OP_UPDATE = "update";
    @NewField
    private static final String OP_REMOVE = "remove";
    @NewField
    private static final String OP_CREATE_INDEX = "createIndex";
    @NewField
    private static final String OP_OTHER = "other";

    DBCollectionImpl(final DBApiLayer db, String name) {
        super(db, name);
    }

    private void instrument(TracedMethod method, String operation, String serverUsed, Integer serverPortUsed) {
        DatastoreParameters params = DatastoreParameters
                .product(DatastoreVendor.MongoDB.name())
                .collection(getName())
                .operation(operation)
                .instance(serverUsed, serverPortUsed)
                .databaseName(getDB().getName())
                .build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
    }

    // CRUD operations

    @Trace(leaf = true)
    QueryResultIterator find(DBObject ref, DBObject fields, int numToSkip, int batchSize, int limit, int options,
            ReadPreference readPref, DBDecoder decoder, DBEncoder encoder) {
        QueryResultIterator result = null;
        try {
            result = Weaver.callOriginal();
            return result;
        } finally {
            String host;
            int port;
            if (null == result) {
                host = getDB().getMongo().getAddress().getHost();
                port = getDB().getMongo().getAddress().getPort();
            } else {
                host = result.getServerAddress().getHost();
                port = result.getServerAddress().getPort();
            }
            instrument(NewRelic.getAgent().getTracedMethod(), OP_FIND, host, port);
        }
    }

    @Trace(leaf = true)
    public WriteResult insert(List<DBObject> list, WriteConcern concern, DBEncoder encoder) {
        instrument(NewRelic.getAgent().getTracedMethod(), OP_INSERT, getDB().getMongo().getAddress().getHost(),
                getDB().getMongo().getAddress().getPort());
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public WriteResult update(DBObject q, DBObject o, boolean upsert, boolean multi, WriteConcern concern,
            DBEncoder encoder) {
        instrument(NewRelic.getAgent().getTracedMethod(), OP_UPDATE, getDB().getMongo().getAddress().getHost(),
                getDB().getMongo().getAddress().getPort());
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public WriteResult remove(DBObject query, WriteConcern concern, DBEncoder encoder) {
        instrument(NewRelic.getAgent().getTracedMethod(), OP_REMOVE, getDB().getMongo().getAddress().getHost(),
                getDB().getMongo().getAddress().getPort());
        return Weaver.callOriginal();
    }

    // Index Creation directly calls DBConnector.doOperation. So we need to trace it.
    // Other index functions go through DB.command.

    @Trace
    public void createIndex(final DBObject keys, final DBObject options, DBEncoder encoder) {
        instrument(NewRelic.getAgent().getTracedMethod(), OP_CREATE_INDEX, getDB().getMongo().getAddress().getHost(),
                getDB().getMongo().getAddress().getPort());
        Weaver.callOriginal();
    }

    // Bulk Operations
    @Trace(metricName = "Java/com.mongodb.DBCollectionImpl/MongoBulkOperation")
    abstract BulkWriteResult executeBulkWriteOperation(final boolean ordered, final List<WriteRequest> requests,
            final WriteConcern writeConcern, final DBEncoder encoder);

    @Trace(leaf = true)
    private BulkWriteResult writeWithCommandProtocol(final DBPort port, final WriteRequest.Type type,
            BaseWriteCommandMessage message, final WriteConcern writeConcern) {
        String operation;
        if (type == WriteRequest.Type.INSERT) {
            operation = OP_INSERT;
        } else if (type == WriteRequest.Type.REMOVE) {
            operation = OP_REMOVE;
        } else if (type == WriteRequest.Type.UPDATE) {
            operation = OP_UPDATE;
        } else {
            operation = OP_OTHER;
        }

        instrument(NewRelic.getAgent().getTracedMethod(), operation, getDB().getMongo().getAddress().getHost(),
                getDB().getMongo().getAddress().getPort());
        return Weaver.callOriginal();
    }

}
