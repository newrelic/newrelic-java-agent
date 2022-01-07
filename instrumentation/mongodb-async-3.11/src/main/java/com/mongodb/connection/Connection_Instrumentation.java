/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.connection;

import com.mongodb.MongoNamespace;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;
import org.bson.codecs.Decoder;

@Weave(type = MatchType.Interface, originalName = "com/mongodb/connection/Connection")
public abstract class Connection_Instrumentation {

    @Trace(leaf = true)
    public <T> QueryResult<T> getMore(MongoNamespace namespace, long cursorId, int numberToReturn,
            Decoder<T> resultDecoder) {

        DatastoreParameters params = DatastoreParameters
                .product(DatastoreVendor.MongoDB.name())
                .collection(namespace.getCollectionName())
                .operation(MongoUtil.OP_GET_MORE)
                .instance(getDescription().getServerAddress().getHost(), getDescription().getServerAddress().getPort())
                .databaseName(namespace.getDatabaseName())
                .build();

        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);

        return Weaver.callOriginal();
    }

    public abstract ConnectionDescription getDescription();

}
