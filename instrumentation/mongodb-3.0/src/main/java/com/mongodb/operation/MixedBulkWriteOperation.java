/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.operation;

import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.WriteRequest;
import com.mongodb.connection.Connection;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

@Weave
public abstract class MixedBulkWriteOperation {

    public abstract MongoNamespace getNamespace();

    @Weave
    private static class Run {

        final MixedBulkWriteOperation this$0 = Weaver.callOriginal();

        private final WriteRequest.Type type = Weaver.callOriginal();

        @Trace(leaf = true)
        BulkWriteResult execute(final Connection connection) {
            String operation;
            if (type == WriteRequest.Type.INSERT) {
                operation = MongoUtil.OP_INSERT;
            } else if (type == WriteRequest.Type.DELETE) {
                operation = MongoUtil.OP_DELETE;
            } else if (type == WriteRequest.Type.UPDATE) {
                operation = MongoUtil.OP_UPDATE;
            } else {
                operation = MongoUtil.OP_DEFAULT;
            }

            String collectionName = this$0.getNamespace().getCollectionName();
            ServerAddress address = connection.getDescription().getConnectionId().getServerId().getAddress();

            DatastoreParameters params = DatastoreParameters
                    .product(DatastoreVendor.MongoDB.name())
                    .collection(collectionName)
                    .operation(operation)
                    .instance(address.getHost(), address.getPort())
                    .databaseName(this$0.getNamespace().getDatabaseName())
                    .build();
            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);

            return Weaver.callOriginal();
        }

    }

}
