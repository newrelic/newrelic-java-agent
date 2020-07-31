/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb;

import com.mongodb.client.MongoDatabase_Weaved;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.MongoClient")
public abstract class MongoClient_Weaved {

    public MongoDatabase_Weaved getDatabase(final String databaseName) {
        MongoDatabase_Weaved collection = Weaver.callOriginal();
        collection.address = getAddress();
        return collection;
    }

    public abstract ServerAddress getAddress();

    public abstract DB getDB(String databaseName);
}
