/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.DBCollection")
public abstract class DBCollection_Weave {

    @Trace(leaf = true)
    public WriteResult update(final DBObject query, final DBObject update, final boolean upsert, final boolean multi,
            final WriteConcern aWriteConcern, final DBEncoder encoder) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_UPDATE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public WriteResult remove(final DBObject query, final WriteConcern writeConcern) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_REMOVE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public List<Cursor> parallelScan(final ParallelScanOptions options) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_PARALLEL_SCAN);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    private Cursor aggregate(final List<? extends DBObject> pipeline, final AggregationOptions options,
            final ReadPreference readPreference, final boolean returnCursorForOutCollection) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_AGGREGATE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    private WriteResult insert(final List<InsertRequest> insertRequestList, final WriteConcern writeConcern,
            final boolean continueOnError) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_INSERT);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public DBCursor find(final DBObject query) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_FIND);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    DBObject findOne(final DBObject query, final DBObject projection, final DBObject sort,
            final ReadPreference readPreference, final long maxTime, final TimeUnit maxTimeUnit) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_FIND);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void createIndex(final DBObject keys, final DBObject options) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_CREATE_INDEX);
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public DBCursor find(final DBObject query, final DBObject projection) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_FIND);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public long count() {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_COUNT);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public long count(final DBObject query) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_COUNT);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public long count(final DBObject query, final ReadPreference projection) {
        instrument(NewRelic.getAgent().getTracedMethod(), MongoUtil.OP_COUNT);
        return Weaver.callOriginal();
    }

    @Trace(metricName = "Java/com.mongodb.DBCollection/MongoBulkOperation")
    abstract BulkWriteResult executeBulkWriteOperation(final boolean ordered, final List<WriteRequest> writeRequests,
            final WriteConcern writeConcern);

    public abstract DB getDB();

    abstract MongoNamespace getNamespace();

    private void instrument(TracedMethod method, String operationName) {
        ServerAddress serverAddress = getDB().getMongo().getAddress();

        DatastoreParameters params = DatastoreParameters
                .product(DatastoreVendor.MongoDB.name())
                .collection(getNamespace().getCollectionName())
                .operation(operationName)
                .instance(serverAddress.getHost(), serverAddress.getPort())
                .databaseName(getDB().getName())
                .build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
    }

}
