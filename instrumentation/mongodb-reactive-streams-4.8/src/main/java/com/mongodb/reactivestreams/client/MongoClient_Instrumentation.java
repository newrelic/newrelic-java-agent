/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.reactivestreams.client;

import com.mongodb.connection.ClusterDescription;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

@Weave(type = MatchType.Interface, originalName = "com.mongodb.reactivestreams.client.MongoClient")
public class MongoClient_Instrumentation {

    /**
     * This is weaved so that we can capture the database name and map it to a host
     */
    public MongoDatabase getDatabase(String dbName) {
        String hosts = MongoUtil.determineHostDisplayValueFromCluster(getClusterDescription());
        MongoUtil.addDatabaseAndHostToMap(dbName, hosts);

        return Weaver.callOriginal();
    }

    public ClusterDescription getClusterDescription() {
        return Weaver.callOriginal();
    }
}
