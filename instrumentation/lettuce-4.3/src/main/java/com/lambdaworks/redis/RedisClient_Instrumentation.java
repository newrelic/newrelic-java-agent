/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.lambdaworks.redis;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
@Weave(originalName = "com.lambdaworks.redis.RedisClient")
public abstract class RedisClient_Instrumentation extends AbstractRedisClient {

    private final RedisURI redisURI = Weaver.callOriginal();

    public static RedisClient_Instrumentation create(String uri) {
        return Weaver.callOriginal();
    }

    public abstract StatefulRedisConnection<String, String> connect();

    protected <K, V> StatefulRedisConnectionImpl_Instrumentation<K, V> newStatefulRedisConnection(CommandHandler<K, V> commandHandler,
                                                                                                  RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        StatefulRedisConnectionImpl_Instrumentation<K, V> connection = Weaver.callOriginal();
        connection.redisURI = redisURI;
        return connection;
    }
}
