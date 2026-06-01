/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.lettuce65.instrumentation;

import com.newrelic.api.agent.DatastoreParameters;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl_Instrumentation;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.StatefulRedisClusterConnectionImpl_Instrumentation;

public class RedisDatastoreParameters {
    public static DatastoreParameters from(RedisURI uri, String operation) {
        DatastoreParameters params;
        if (uri != null) {
            params = DatastoreParameters.product("Redis").collection(null).operation(operation)
                    .instance(uri.getHost(), uri.getPort()).databaseName(String.valueOf(uri.getDatabase())).build();
        } else {
            params = DatastoreParameters.product("Redis").collection(null).operation(operation).noInstance()
                    .noDatabaseName().noSlowQuery().build();
        }
        return params;
    }

    /***
     * Utility method for retrieving the URI off an instrumented connection object (we add this URI as a NewField in our instrumentation).
     *
     * @param conn A Redis StatefulConnection instance.
     * @return The external URI to associate with this connection. If the connection is null or uninstrumented, returns null.
     */
    public static RedisURI getUriFromConnection(StatefulConnection<?, ?> conn){
        if (conn == null){
            return null;
        }
        if (conn instanceof StatefulRedisConnectionImpl_Instrumentation) {
            return ((StatefulRedisConnectionImpl_Instrumentation) conn).redisURI;
        }
        if (conn instanceof StatefulRedisClusterConnectionImpl_Instrumentation) {
            return ((StatefulRedisClusterConnectionImpl_Instrumentation) conn).firstSeedUri;
        }
        return null;
    }
}
