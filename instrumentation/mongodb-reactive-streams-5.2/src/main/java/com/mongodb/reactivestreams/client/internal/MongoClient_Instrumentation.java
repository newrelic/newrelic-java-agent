/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.reactivestreams.client.internal;

import com.mongodb.connection.ClusterDescription;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

/* Since you cannot weave indirect classes in an interface, I chose to weave an implementation class */
@Weave(type = MatchType.ExactClass, originalName = "com.mongodb.reactivestreams.client.internal.MongoClientImpl")
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
