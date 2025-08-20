/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.internal.operation;

import com.mongodb.MongoNamespace;
import com.mongodb.WriteConcern;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.nr.agent.mongo.MongoUtil;
import org.bson.codecs.Decoder;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.internal.operation.FindAndDeleteOperation")
public class FindAndDeleteOperation_Instrumentation<T> extends BaseFindAndModifyOperation_Instrumentation<T> {

    public FindAndDeleteOperation_Instrumentation(final MongoNamespace namespace, final WriteConcern writeConcern, final boolean retryWrites,
            final Decoder<T> decoder) {
        super.collectionName = namespace.getCollectionName();
        super.databaseName = namespace.getDatabaseName();
        super.operationName = MongoUtil.OP_FIND_AND_DELETE;
    }
}
