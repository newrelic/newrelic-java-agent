/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.internal.operation;

import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.WriteModel;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

import java.util.List;

import static com.nr.agent.mongo.MongoUtil.OP_BULK_WRITE;
import static com.nr.agent.mongo.MongoUtil.OP_INSERT_MANY;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.internal.operation.Operations")
final class Operations_Instrumentation<TDocument> {

    MixedBulkWriteOperation_Instrumentation insertMany(final List<? extends TDocument> documents, final InsertManyOptions options) {
        MixedBulkWriteOperation_Instrumentation op = Weaver.callOriginal();
        op.operationName = OP_INSERT_MANY;
        return op;
    }

    /**
     * Because all the write operations proxy to this method (except insertMany), we can instrument a single method and assign
     * the operation name passed on the length of the <code>requests</code> {@link List List}.
     * <ul>
     *     <li>if the list has a single entry, interrogate its type and assign the operation name accordingly</li>
     *     <li>if the list has more than one entry, the operation name will be <code>OP_BULK_WRITE</code></li>
     * </ul>
     */
    MixedBulkWriteOperation_Instrumentation bulkWrite(final List<? extends WriteModel<? extends TDocument>> requests, final BulkWriteOptions options) {
        String operationName  = requests.size() > 1 ? OP_BULK_WRITE : MongoUtil.determineBulkWriteOperation(requests.get(0));
        MixedBulkWriteOperation_Instrumentation op = Weaver.callOriginal();
        op.operationName = operationName;
        return op;
    }
}
