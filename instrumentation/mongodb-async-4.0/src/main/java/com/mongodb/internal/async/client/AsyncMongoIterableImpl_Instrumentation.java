/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb.internal.async.client;

import com.mongodb.Block;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.internal.async.AsyncBatchCursor;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.lang.Nullable;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;
import com.nr.agent.mongo.NRCallbackWrapper;

import java.util.Collection;

@Weave(type = MatchType.BaseClass, originalName = "com/mongodb/internal/async/client/AsyncMongoIterableImpl")
abstract class AsyncMongoIterableImpl_Instrumentation<TResult> {

    @NewField
    protected String collectionName = MongoUtil.UNKNOWN;

    @NewField
    protected String databaseName = MongoUtil.UNKNOWN;

    @NewField
    protected String operationName = MongoUtil.UNKNOWN;

    AsyncMongoIterableImpl_Instrumentation(@Nullable final AsyncClientSession clientSession, final OperationExecutor executor,
            final ReadConcern readConcern, final ReadPreference readPreference, final boolean retryReads) {
    }

    @Trace
    public void first(SingleResultCallback<TResult> callback) {
        if (!(callback instanceof NRCallbackWrapper)) {
            NRCallbackWrapper<TResult> wrapper = new NRCallbackWrapper<>(callback);
            wrapper.token = NewRelic.getAgent().getTransaction().getToken();

            DatastoreParameters datastoreParameters;

            if (databaseName != null) {
                datastoreParameters = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
                        .collection(collectionName)
                        .operation(operationName)
                        .noInstance()
                        .databaseName(databaseName)
                        .build();
            } else {
                datastoreParameters = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
                        .collection(collectionName)
                        .operation(operationName)
                        .build();
            }

            wrapper.params = datastoreParameters;
            wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
            callback = wrapper;
        }
        Weaver.callOriginal();
    }

    @Trace
    public void forEach(Block<? super TResult> block, SingleResultCallback<Void> callback) {
        if (!(callback instanceof NRCallbackWrapper)) {
            NRCallbackWrapper<Void> wrapper = new NRCallbackWrapper<>(callback);
            wrapper.token = NewRelic.getAgent().getTransaction().getToken();
            wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
                    .collection(collectionName)
                    .operation(operationName)
                    .noInstance()
                    .databaseName(databaseName)
                    .build();
            wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
            callback = wrapper;
        }
        Weaver.callOriginal();
    }

    @Trace
    public <A extends Collection<? super TResult>> void into(A target, SingleResultCallback<A> callback) {
        if (!(callback instanceof NRCallbackWrapper)) {
            NRCallbackWrapper<A> wrapper = new NRCallbackWrapper<>(callback);
            wrapper.token = NewRelic.getAgent().getTransaction().getToken();
            wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
                    .collection(collectionName)
                    .operation(operationName)
                    .noInstance()
                    .databaseName(databaseName)
                    .build();
            wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
            callback = wrapper;
        }
        Weaver.callOriginal();
    }

    @Trace
    public void batchCursor(SingleResultCallback<AsyncBatchCursor<TResult>> callback) {
        if (!(callback instanceof NRCallbackWrapper)) {
            NRCallbackWrapper<AsyncBatchCursor<TResult>> wrapper = new NRCallbackWrapper<>(callback);
            wrapper.token = NewRelic.getAgent().getTransaction().getToken();
            wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
                    .collection(collectionName)
                    .operation(operationName)
                    .noInstance()
                    .databaseName(databaseName)
                    .build();
            wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
            callback = wrapper;
        }
        Weaver.callOriginal();
    }

}
