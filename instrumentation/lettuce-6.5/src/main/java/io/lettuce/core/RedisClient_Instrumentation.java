/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.lettuce.core;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.core.resource.ClientResources;

import java.time.Duration;

@Weave(originalName = "io.lettuce.core.RedisClient")
public abstract class RedisClient_Instrumentation extends AbstractRedisClient {

    private final RedisURI redisURI = Weaver.callOriginal();

    protected RedisClient_Instrumentation(ClientResources clientResources, RedisURI redisURI) {
        super(clientResources);
    }

    public static RedisClient_Instrumentation create(String uri) {
        return Weaver.callOriginal();
    }

    public abstract StatefulRedisConnection<String, String> connect();

    protected <K, V> StatefulRedisConnectionImpl_Instrumentation<K, V> newStatefulRedisConnection(RedisChannelWriter channelWriter,
            PushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout) {
        StatefulRedisConnectionImpl_Instrumentation<K, V> connection = Weaver.callOriginal();
        connection.redisURI = redisURI;
        return connection;
    }
}
