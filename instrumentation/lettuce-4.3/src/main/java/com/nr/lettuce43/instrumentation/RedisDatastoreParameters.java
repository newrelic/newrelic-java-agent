package com.nr.lettuce43.instrumentation;

import com.lambdaworks.redis.RedisURI;
import com.newrelic.api.agent.DatastoreParameters;

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
}
