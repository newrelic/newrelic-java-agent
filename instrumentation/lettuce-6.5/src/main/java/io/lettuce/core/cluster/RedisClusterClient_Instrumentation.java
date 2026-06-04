/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.lettuce.core.cluster;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.RedisCodec;

import java.util.Iterator;

@Weave(originalName = "io.lettuce.core.cluster.RedisClusterClient")
public class RedisClusterClient_Instrumentation {

    private final Iterable<RedisURI> initialUris = Weaver.callOriginal();

    public <K, V> StatefulRedisClusterConnection<K, V> connect(RedisCodec<K, V> codec) {
        StatefulRedisClusterConnection<K, V> connection = Weaver.callOriginal();
        if (initialUris != null && connection instanceof StatefulRedisClusterConnectionImpl_Instrumentation) {
            StatefulRedisClusterConnectionImpl_Instrumentation connImpl = (StatefulRedisClusterConnectionImpl_Instrumentation) connection;
            Iterator<RedisURI> it = initialUris.iterator();
            connImpl.firstSeedUri = it.hasNext() ? it.next() : null;
        }
        return connection;
    }

}
