/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.lettuce.core;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.lettuce65.instrumentation.NRBiConsumer;
import com.nr.lettuce65.instrumentation.RedisDatastoreParameters;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;

@Weave(originalName = "io.lettuce.core.AbstractRedisAsyncCommands")
public abstract class AbstractRedisAsyncCommands_Instrumentation<K, V> {

    public abstract StatefulConnection<K, V> getConnection();

    @SuppressWarnings("unchecked")
    @Trace
    public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {
        AsyncCommand<K, V, T> acmd = Weaver.callOriginal();
        RedisURI uri = null;

        StatefulConnection<K, V> conn = getConnection();
        if (StatefulRedisConnectionImpl_Instrumentation.class.isInstance(conn)) {
            StatefulRedisConnectionImpl_Instrumentation<K, V> connImpl = (StatefulRedisConnectionImpl_Instrumentation<K, V>) conn;
            if (connImpl.redisURI != null) {
                uri = connImpl.redisURI;
            }
        }
        String operation = "UnknownOp";
        ProtocolKeyword t = cmd.getType();
        if (t != null && t.toString() != null && !t.toString().isEmpty()) {
            operation = t.toString();
        }
        if (operation.equalsIgnoreCase("expire")) {
            return acmd;
        }
        DatastoreParameters params = RedisDatastoreParameters.from(uri, operation);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Redis", operation);
        NRBiConsumer<T> nrBiConsumer = new NRBiConsumer<T>(segment, params);
        acmd.whenComplete(nrBiConsumer);
        return acmd;
    }

}
