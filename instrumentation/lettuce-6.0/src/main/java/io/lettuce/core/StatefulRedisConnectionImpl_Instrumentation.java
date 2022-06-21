/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.lettuce.core;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.PushHandler;

import java.time.Duration;

@Weave(originalName = "io.lettuce.core.StatefulRedisConnectionImpl")
public abstract class StatefulRedisConnectionImpl_Instrumentation<K, V> implements StatefulConnection<K, V> {

    @NewField
    public RedisURI redisURI = null;

    public StatefulRedisConnectionImpl_Instrumentation(RedisChannelWriter writer, PushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout) {

    }
}
