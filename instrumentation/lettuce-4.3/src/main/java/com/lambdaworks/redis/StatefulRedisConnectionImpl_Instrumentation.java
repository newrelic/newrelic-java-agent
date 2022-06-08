/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.lambdaworks.redis;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

import java.util.concurrent.TimeUnit;

@Weave(originalName = "com.lambdaworks.redis.StatefulRedisConnectionImpl")
public abstract class StatefulRedisConnectionImpl_Instrumentation<K, V> implements StatefulConnection<K, V> {

    @NewField
    public RedisURI redisURI = null;

    public StatefulRedisConnectionImpl_Instrumentation(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {

    }
}
