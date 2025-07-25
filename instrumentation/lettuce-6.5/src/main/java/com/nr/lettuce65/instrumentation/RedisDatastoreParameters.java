/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.lettuce65.instrumentation;

import com.newrelic.api.agent.DatastoreParameters;
import io.lettuce.core.RedisURI;

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
