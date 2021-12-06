/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.async.client;

import com.mongodb.Block;
import com.mongodb.MongoNamespace;
import com.mongodb.async.AsyncBatchCursor;
import com.mongodb.async.SingleResultCallback;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;
import com.nr.agent.mongo.NRCallbackWrapper;

import java.util.Collection;

@Weave(type = MatchType.ExactClass, originalName = "com/mongodb/async/client/FindIterableImpl")
abstract class FindIterableImpl_Instrumentation<TResult> {

    private final MongoNamespace namespace = Weaver.callOriginal();

    @Trace
    public void first(SingleResultCallback<TResult> callback) {

        if (!(callback instanceof NRCallbackWrapper)) {
            NRCallbackWrapper<TResult> wrapper = new NRCallbackWrapper<TResult>(callback);
            wrapper.token = NewRelic.getAgent().getTransaction().getToken();
            wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
                    .collection(namespace.getCollectionName())
                    .operation(MongoUtil.OP_FIND)
                    .noInstance()
                    .databaseName(namespace.getDatabaseName())
                    .build();
            wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(MongoUtil.OP_FIND);
            callback = wrapper;
        }
        Weaver.callOriginal();
    }

    @Trace
    public void forEach(Block<? super TResult> block, SingleResultCallback<Void> callback) {
        if (!(callback instanceof NRCallbackWrapper)) {
            NRCallbackWrapper<Void> wrapper = new NRCallbackWrapper<Void>(callback);
            wrapper.token = NewRelic.getAgent().getTransaction().getToken();
            wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
                    .collection(namespace.getCollectionName())
                    .operation(MongoUtil.OP_FIND)
                    .noInstance()
                    .databaseName(namespace.getDatabaseName())
                    .build();
            wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(MongoUtil.OP_FIND);
            callback = wrapper;
        }
        Weaver.callOriginal();
    }

    @Trace
    public <A extends Collection<? super TResult>> void into(A target, SingleResultCallback<A> callback) {
        if (!(callback instanceof NRCallbackWrapper)) {
            NRCallbackWrapper<A> wrapper = new NRCallbackWrapper<A>(callback);
            wrapper.token = NewRelic.getAgent().getTransaction().getToken();
            wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
                    .collection(namespace.getCollectionName())
                    .operation(MongoUtil.OP_FIND)
                    .noInstance()
                    .databaseName(namespace.getDatabaseName())
                    .build();
            wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(MongoUtil.OP_FIND);
            callback = wrapper;
        }
        Weaver.callOriginal();
    }

    @Trace
    public void batchCursor(SingleResultCallback<AsyncBatchCursor<TResult>> callback) {
        if (!(callback instanceof NRCallbackWrapper)) {
            NRCallbackWrapper<AsyncBatchCursor<TResult>> wrapper = new NRCallbackWrapper<AsyncBatchCursor<TResult>>(callback);
            wrapper.token = NewRelic.getAgent().getTransaction().getToken();
            wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
                    .collection(namespace.getCollectionName())
                    .operation(MongoUtil.OP_FIND)
                    .noInstance()
                    .databaseName(namespace.getDatabaseName())
                    .build();
            wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(MongoUtil.OP_FIND);
            callback = wrapper;
        }
        Weaver.callOriginal();
    }

}
