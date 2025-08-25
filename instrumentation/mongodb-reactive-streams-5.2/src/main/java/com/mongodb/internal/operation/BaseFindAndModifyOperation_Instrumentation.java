/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.internal.operation;

import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.binding.AsyncWriteBinding;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

@Weave(type = MatchType.BaseClass, originalName = "com.mongodb.internal.operation.BaseFindAndModifyOperation")
public class BaseFindAndModifyOperation_Instrumentation <T> implements AsyncWriteOperation<T> {

    @NewField
    protected String collectionName;

    @NewField
    protected String databaseName;

    @NewField
    protected String operationName;

    @Override
    public void executeAsync(AsyncWriteBinding binding, SingleResultCallback<T> callback) {
        callback = MongoUtil.instrumentSingleResultCallback(callback, collectionName, operationName, databaseName,
                MongoUtil.getHostBasedOnDatabaseName(databaseName));
        Weaver.callOriginal();
    }
}
